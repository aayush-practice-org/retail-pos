package io.aygh.billing.service;

import io.aygh.sales.dto.response.SaleDetailResponse;
import io.aygh.sales.dto.response.SaleItemResponse;
import io.aygh.sales.dto.response.SalesBookResponse;
import io.aygh.sales.dto.response.SalesBookRowResponse;
import io.aygh.sales.dto.response.SalesBookTotalResponse;
import io.aygh.sales.helper.SaleCalculator;
import io.aygh.shared.entity.TaxScheme;
import io.aygh.shared.print.PosLayout;
import io.aygh.shared.print.PosPaper;
import io.aygh.shared.print.PosPrintException;
import io.aygh.shared.print.PosText;
import io.aygh.shared.response.PrintPaperType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders sales as printable IRD invoices (responsive A4/A5/A6 Schedule 5 & POS thermal roll)
 * and generates the IRD Sales Book (A4 landscape).
 *
 * <h2>PAN and VAT billing</h2>
 * What a mart may print depends on how it was registered with the IRD when the bill was
 * raised, which the sale carries as its {@link TaxScheme}. There are two forms:
 * <ul>
 *   <li><b>VAT bill</b> ({@link #renderVatInvoice}) — a <em>Tax Invoice</em> that breaks the
 *   bill into taxable amount and 13% VAT. Every figure above the VAT row is exclusive of
 *   VAT, so a mart whose shelf prices already carry VAT prints its lines, unit prices and
 *   discount at what they are worth before it; the total stays what was charged.</li>
 *   <li><b>PAN bill</b> ({@link #renderPanInvoice}) — a plain <em>Invoice</em> that stops at
 *   the total: no taxable split, no VAT row. While CBMS syncing is switched off nothing has
 *   been filed under the mart's PAN either, so it is stamped {@link #PAN_BILL_NOTICE} under
 *   the title, on the roll and on the page alike, and is never taken for a tax document.
 *   Once {@code CbmsClientImpl} posts bills for real, this notice is the thing to revisit.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfService {

    /**
     * Stamped under the title of a PAN bill. Upper case to match the COPY OF ORIGINAL
     * stamp it sits beside on a reprint.
     */
    private static final String PAN_BILL_NOTICE = "THIS IS NOT A VAT OR PAN BILL";

    // ── Fonts (Standard Type-1, always available, no font embedding required) ─────
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    // ── A4 Layout constants ───────────────────────────────────────────────────────
    private static final float MARGIN = 50f;
    private static final float A4_FOOTER_RESERVE = 60f;

    // ── Font sizes & spacing ──────────────────────────────────────────────────────
    private static final float SIZE_TITLE = 20f;
    private static final float SIZE_SUBTITLE = 12f;
    private static final float SIZE_BODY = 10f;
    private static final float SIZE_SMALL = 8f;
    private static final float LINE_HEIGHT = 16f;
    private static final float SECTION_GAP = 12f;
    private static final float DIVIDER_THICKNESS = 0.5f;
    private static final float ASCENT_RATIO = 0.75f;

    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Kathmandu");
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm").withZone(REPORT_ZONE);
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(REPORT_ZONE);

    // ── Sales Book Layout Constants ───────────────────────────────────────────────
    private static final PDRectangle A4_LANDSCAPE =
            new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    private static final float LANDSCAPE_WIDTH = A4_LANDSCAPE.getWidth();
    private static final float LANDSCAPE_HEIGHT = A4_LANDSCAPE.getHeight();
    private static final float LANDSCAPE_CONTENT_WIDTH = LANDSCAPE_WIDTH - 2 * MARGIN;

    private static final float[] BOOK_COL_FRACTIONS = {
            0f, 0.075f, 0.165f, 0.320f, 0.435f, 0.530f, 0.625f, 0.720f, 0.815f, 0.905f, 1f
    };

    private static final String[][] BOOK_HEADER = {
            {"Date"}, {"Bill", "No"}, {"Buyer's", "Name"}, {"Buyer's PAN", "Number"},
            {"Total", "Sales"}, {"Non Taxable", "Sales"}, {"Export", "Sales"}, {"Discount"},
            {"Taxable Amount"}, {"Tax (Rs)"}
    };
    private static final boolean[] BOOK_HEADER_ALIGN = {
            false, false, false, false, false, false, false, false, false, false
    };
    private static final boolean[] BOOK_ROW_ALIGN = {
            false, false, false, false, true, true, true, true, true, true
    };

    private final MartBrandingService brandingService;

    // ═══════════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Renders the bill on the requested paper type (MM80, MM75, A4, A5, A6), as a VAT bill
     * or a PAN bill according to the scheme the sale was raised under.
     */
    public byte[] renderTaxInvoice(SaleDetailResponse sale, PrintPaperType paperType) {
        return sale.taxScheme() == TaxScheme.VAT
                ? renderVatInvoice(sale, paperType)
                : renderPanInvoice(sale, paperType);
    }

    /**
     * The VAT bill: a Tax Invoice with taxable amount and VAT rows.
     */
    public byte[] renderVatInvoice(SaleDetailResponse sale, PrintPaperType paperType) {
        return render(sale, paperType, BillForm.vat(sale));
    }

    /**
     * The PAN bill: a plain Invoice, no VAT, stamped {@link #PAN_BILL_NOTICE}.
     */
    public byte[] renderPanInvoice(SaleDetailResponse sale, PrintPaperType paperType) {
        return render(sale, paperType, BillForm.pan());
    }

    /**
     * The filed copy: A4, itemised, with the mart's registration details.
     */
    public byte[] renderA4(SaleDetailResponse sale) {
        return renderTaxInvoice(sale, PrintPaperType.A4);
    }

    /**
     * The counter copy, on whatever roll the till feeds.
     */
    public byte[] renderReceipt(SaleDetailResponse sale, PosPaper paper) {
        return renderReceipt(sale, paper, BillForm.of(sale));
    }

    private byte[] render(SaleDetailResponse sale, PrintPaperType paperType, BillForm form) {
        PrintPaperType type = paperType != null ? paperType : PrintPaperType.MM80;
        if (type.isPos()) {
            return renderReceipt(sale, type.toPosPaper(), form);
        }
        return renderPageTaxInvoice(sale, type, form);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PAN / VAT bill forms
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * What sets a PAN bill apart from a VAT bill on paper.
     *
     * @param title     printed at the head of the form
     * @param vat       whether the taxable amount and VAT rows are printed
     * @param stripVat  whether the line figures carry VAT that has to come out of them
     * @param notice    stamped under the title, or null
     */
    private record BillForm(String title, boolean vat, boolean stripVat, String notice) {

        static BillForm of(SaleDetailResponse sale) {
            return sale.taxScheme() == TaxScheme.VAT ? vat(sale) : pan();
        }

        static BillForm pan() {
            return new BillForm("Invoice", false, false, PAN_BILL_NOTICE);
        }

        static BillForm vat(SaleDetailResponse sale) {
            return new BillForm("Tax Invoice", true, taxIncluded(sale), null);
        }

        /**
         * Whether the shelf prices on this bill carried VAT. Read off the bill itself
         * rather than today's CBMS setup, so a reprint prints as it was billed: VAT
         * added on top makes the total exceed the discounted basket, VAT taken out of
         * the price leaves the two equal.
         */
        private static boolean taxIncluded(SaleDetailResponse sale) {
            if (sale.netTotal() == null || sale.subTotal() == null || !isPositive(sale.vatAmount())) {
                return false;
            }
            BigDecimal discount = sale.discountAmount() != null ? sale.discountAmount() : BigDecimal.ZERO;
            return sale.netTotal().compareTo(sale.subTotal().subtract(discount)) == 0;
        }
    }

    /**
     * The figures printed above the summary rows. On a bill whose prices carried VAT
     * every one of them is shown exclusive of it, so reading the form down — items,
     * less discount, taxable amount — adds up.
     */
    private record PrintedFigures(List<BigDecimal> rates, List<BigDecimal> lineDiscounts,
                                  List<BigDecimal> lineTotals, BigDecimal subTotal, BigDecimal discount) {
    }

    /**
     * Each line is rounded to the paisa on its own, so on a VAT-inclusive bill their sum
     * can land a paisa either side of the amount the bill was actually taxed on. That
     * amount stands, and the drift is carried on the last line charged for at all.
     */
    private static PrintedFigures printedFigures(SaleDetailResponse sale, BillForm form) {
        List<SaleItemResponse> items = sale.items() != null ? sale.items() : List.of();
        BigDecimal discount = sale.discountAmount() != null ? sale.discountAmount() : BigDecimal.ZERO;

        List<BigDecimal> rates = new ArrayList<>();
        List<BigDecimal> lineDiscounts = new ArrayList<>();
        List<BigDecimal> lineTotals = new ArrayList<>();
        for (SaleItemResponse item : items) {
            rates.add(printed(item.rate(), form));
            lineDiscounts.add(printed(item.discountAmount(), form));
            lineTotals.add(printed(item.lineTotal(), form));
        }

        if (!form.stripVat()) {
            return new PrintedFigures(rates, lineDiscounts, lineTotals, sale.subTotal(), discount);
        }

        BigDecimal printedDiscount = SaleCalculator.excludeVat(discount);
        BigDecimal subTotal = sale.taxableAmount().add(printedDiscount);

        int lastCharged = -1;
        for (int i = lineTotals.size() - 1; i >= 0; i--) {
            if (lineTotals.get(i).signum() != 0) {
                lastCharged = i;
                break;
            }
        }
        if (lastCharged >= 0) {
            BigDecimal drift = subTotal.subtract(lineTotals.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            lineTotals.set(lastCharged, lineTotals.get(lastCharged).add(drift));
        }

        return new PrintedFigures(rates, lineDiscounts, lineTotals, subTotal, printedDiscount);
    }

    private static BigDecimal printed(BigDecimal amount, BillForm form) {
        BigDecimal value = amount != null ? amount : BigDecimal.ZERO;
        return form.stripVat() ? SaleCalculator.excludeVat(value) : value;
    }

    /**
     * Renders the IRD invoice form on a thermal roll, laid out the same way as the restaurant
     * kiosk's roll invoice: header, stamps, seller / bill / buyer fields, payment method, one
     * block per line, the summary rows, amount in words, signature and footer.
     * <p>
     * The form's five columns (S.No. / Details / Quantity / Per Unit / Total) cannot sit side
     * by side across a roll, so each line prints as a block instead: serial number and
     * description first, quantity and unit price under them, the line total against the right
     * edge. All content is wrapped to the content column ONCE and that same wrapped content is
     * both measured and drawn, so the roll can never be cut short of the print.
     */
    private byte[] renderReceipt(SaleDetailResponse sale, PosPaper paper, BillForm form) {
        MartBranding branding = brandingService.resolve();
        PosLayout layout = PosLayout.of(paper);
        log.debug("Rendering {} receipt for {}", paper, sale.invoiceNumber());

        int printNumber = printNumber(sale);
        List<SaleItemResponse> items = sale.items() != null ? sale.items() : List.of();
        PrintedFigures figures = printedFigures(sale, form);

        PosHeaderContent header = posHeaderContent(branding, form.title(), layout);

        // What is stamped under the title: the copy number on a reprint, and on a PAN bill
        // the notice that it is not a VAT bill. Both are measured from this one list.
        List<String> stampLines = new ArrayList<>();
        if (printNumber > 1) {
            stampLines.addAll(PosText.wrap(FONT_BOLD, layout.subtitleSize(),
                    "COPY OF ORIGINAL (" + printNumber + ")", layout.contentWidth()));
        }
        if (form.notice() != null) {
            stampLines.addAll(PosText.wrap(FONT_BOLD, layout.subtitleSize(),
                    form.notice(), layout.contentWidth()));
        }

        // The mart's name and PAN are already printed in the header, so only the address —
        // which the header does not carry — is repeated as a field.
        List<String> sellerMeta = new ArrayList<>();
        addPosMetaLine(sellerMeta, "Seller's Address", branding.companyAddress(), layout);

        String txnDate = transactionDate(sale);
        List<String> billMeta = new ArrayList<>();
        addPosMetaLine(billMeta, "Bill Number", sale.invoiceNumber(), layout);
        addPosMetaLine(billMeta, "Fiscal Year", sale.fiscalYear(), layout);
        addPosMetaLine(billMeta, "Transactions Date", txnDate, layout);
        addPosMetaLine(billMeta, "Invoice Issue Date", txnDate, layout);
        addPosMetaLine(billMeta, "Print Count", printNumber, layout);
        addPosMetaLine(billMeta, "Payment Status", enumLabel(sale.paymentStatus()), layout);

        List<String> buyerMeta = new ArrayList<>();
        addPosMetaLine(buyerMeta, "Purchaser's Name", sale.customerName(), layout);
        addPosMetaLine(buyerMeta, "Purchaser's PAN", sale.customerPan(), layout);

        List<List<String>> metaBlocks = List.of(sellerMeta, billMeta, buyerMeta);
        List<List<PosRun>> paymentLines = posPaymentMethodLines(sale.paymentMethod(), layout);
        List<PosItemBlock> blocks = buildPosItemBlocks(items, figures, layout);

        List<String> wordsLines = PosText.wrap(FONT_REGULAR, layout.smallSize(),
                "( In words : " + amountInWords(sale.netTotal()) + " )", layout.contentWidth());

        // Discount and total always print; the taxable/VAT pair only on a VAT bill.
        int summaryRows = form.vat() ? 4 : 2;
        List<String> footerLines = posFooterLines("This is a computer generated invoice.", layout);

        float pageHeight = posInvoiceHeight(header, stampLines, metaBlocks, paymentLines,
                blocks, summaryRows, wordsLines, footerLines, layout);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(layout.width(), pageHeight));
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = pageHeight - layout.margin();

                y = drawPosHeader(cs, y, header, layout);

                for (String line : stampLines) {
                    PosText.drawCentered(cs, FONT_BOLD, layout.subtitleSize(), line, y,
                            layout.margin(), layout.contentWidth());
                    y -= layout.subtitleSize() + 3;
                }
                if (!stampLines.isEmpty()) {
                    y -= layout.sectionGap() / 2;
                }

                for (List<String> metaBlock : metaBlocks) {
                    if (metaBlock.isEmpty()) continue;
                    y = drawPosMetaLines(cs, y, metaBlock, layout);
                    y -= layout.sectionGap() / 2;
                }

                y = drawPosRunLines(cs, y, paymentLines, layout);
                y -= layout.sectionGap() / 2;

                y = drawPosDivider(cs, y, layout);
                y = drawPosItemsHeader(cs, y, layout);
                y = drawPosDivider(cs, y, layout);

                for (PosItemBlock block : blocks) {
                    y = drawPosItemBlock(cs, y, block, layout);
                }

                y = drawPosDivider(cs, y, layout);
                y -= layout.sectionGap() / 2;

                y = drawPosSummaryRow(cs, y, "Discount", money(figures.discount()), false, layout);
                if (form.vat()) {
                    y = drawPosSummaryRow(cs, y, "Taxable Amount", money(sale.taxableAmount()), false, layout);
                    y = drawPosSummaryRow(cs, y, "VAT 13 %", money(sale.vatAmount()), false, layout);
                }
                y = drawPosSummaryRow(cs, y, "Total", money(sale.netTotal()), true, layout);

                y -= layout.sectionGap() / 2;
                for (String line : wordsLines) {
                    PosText.drawAt(cs, FONT_REGULAR, layout.smallSize(), line, layout.margin(), y);
                    y -= layout.smallSize() + 3;
                }

                y -= layout.sectionGap();
                y = drawPosSignature(cs, y, layout);

                drawPosFooter(cs, footerLines, y, layout);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate thermal invoice PDF for #{}", sale.invoiceNumber(), e);
            throw new PosPrintException("Could not render receipt for " + sale.invoiceNumber(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Responsive Page Invoice (A4, A5, A6 - Schedule 5 Boxed Table Layout)
    // ═══════════════════════════════════════════════════════════════════════

    private byte[] renderPageTaxInvoice(SaleDetailResponse sale, PrintPaperType paperType, BillForm form) {
        boolean vatRegistered = form.vat();
        String title = form.title();
        MartBranding branding = brandingService.resolve();

        PDRectangle pageSize = paperType.toPageSize();
        float pageWidth = pageSize.getWidth();
        float pageHeight = pageSize.getHeight();

        float scale = pageWidth / PDRectangle.A4.getWidth();
        float margin = Math.max(16f, 36f * scale);
        float contentWidth = pageWidth - 2 * margin;
        float contentRight = margin + contentWidth;

        float titleSize = Math.max(11f, 16f * scale);
        float subtitleSize = Math.max(8.5f, 11f * scale);
        float labelSize = Math.max(6.5f, 8.5f * scale);
        float bodySize = Math.max(6f, 8f * scale);
        float smallSize = Math.max(5.5f, 7f * scale);
        float lineHeight = Math.max(9.5f, 13f * scale);
        float sectionGap = Math.max(4f, 8f * scale);
        float cellPad = Math.max(2f, 4f * scale);
        float lineDrop = bodySize + 3f;

        List<SaleItemResponse> items = sale.items() != null ? sale.items() : List.of();
        PrintedFigures figures = printedFigures(sale, form);

        float[] xs = {
                margin,
                margin + contentWidth * 0.08f,
                margin + contentWidth * 0.52f,
                margin + contentWidth * 0.64f,
                margin + contentWidth * 0.80f,
                margin + contentWidth
        };

        String[][] headerCells = {
                {"S.No."},
                {"Details"},
                {"Quantity"},
                {"Per Unit (Rs)"},
                {"Total Amount (Rs)"}
        };
        boolean[] colAlign = {false, false, true, true, true};

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            try {
                float y = pageHeight - margin - (titleSize + 2f) * ASCENT_RATIO;

                // 1. Business Header
                String companyName = upper(branding.companyName());
                PosText.drawCentered(cs, FONT_BOLD, titleSize + 2f, companyName, y, margin, contentWidth);
                y -= (titleSize + 2f) + 4f;

                String address = notBlank(branding.companyAddress()) ? branding.companyAddress() : "";
                String contact = buildContactLine(branding);
                String businessContact = address;
                if (!contact.isBlank()) {
                    businessContact = businessContact.isEmpty() ? contact : businessContact + "  |  " + contact;
                }
                if (!businessContact.isBlank()) {
                    for (String line : PosText.wrap(FONT_REGULAR, smallSize + 1f, businessContact, contentWidth)) {
                        PosText.drawCentered(cs, FONT_REGULAR, smallSize + 1f, line, y, margin, contentWidth);
                        y -= (smallSize + 1f) + 3f;
                    }
                }

                if (notBlank(branding.registrationNumber())) {
                    PosText.drawCentered(cs, FONT_BOLD, smallSize + 1f, "PAN: " + branding.registrationNumber(), y, margin, contentWidth);
                    y -= (smallSize + 1f) + 4f;
                }

                y -= 4f;
                cs.setLineWidth(DIVIDER_THICKNESS);
                cs.moveTo(margin, y);
                cs.lineTo(contentRight, y);
                cs.stroke();

                y -= (sectionGap + titleSize * ASCENT_RATIO + 3f);

                // 2. Document Title
                PosText.drawCentered(cs, FONT_BOLD, titleSize, title, y, margin, contentWidth);
                y -= (titleSize * 0.35f + 4f);

                int printNumber = printNumber(sale);
                if (printNumber > 1) {
                    y -= (subtitleSize * ASCENT_RATIO + 2f);
                    PosText.drawCentered(cs, FONT_BOLD, subtitleSize,
                            "COPY OF ORIGINAL (" + printNumber + ")", y, margin, contentWidth);
                    y -= (subtitleSize * 0.35f + 4f);
                }

                if (form.notice() != null) {
                    y -= (subtitleSize * ASCENT_RATIO + 2f);
                    PosText.drawCentered(cs, FONT_BOLD, subtitleSize,
                            form.notice(), y, margin, contentWidth);
                    y -= (subtitleSize * 0.35f + 4f);
                }

                y -= 2f;
                cs.setLineWidth(DIVIDER_THICKNESS);
                cs.moveTo(margin, y);
                cs.lineTo(contentRight, y);
                cs.stroke();

                y -= (sectionGap + labelSize * ASCENT_RATIO + 3f);

                // 3. Two-column Metadata
                float leftX = margin;
                float rightX = margin + contentWidth * 0.56f;
                float yMetaStart = y;
                float yL = yMetaStart;

                yL = drawPageTaxField(cs, leftX, yL, "Bill Number", sale.invoiceNumber(), labelSize, bodySize);
                yL = drawPageTaxField(cs, leftX, yL, "Seller's PAN", branding.registrationNumber(), labelSize, bodySize);
                yL = drawPageTaxField(cs, leftX, yL, "Seller's Name", branding.companyName(), labelSize, bodySize);
                if (notBlank(branding.companyPhone())) {
                    yL = drawPageTaxField(cs, leftX, yL, "Seller's Phone", branding.companyPhone(), labelSize, bodySize);
                }
                yL = drawPageTaxField(cs, leftX, yL, "Address", branding.companyAddress(), labelSize, bodySize);
                yL = drawPageTaxField(cs, leftX, yL, "Purchaser's Name", sale.customerName(), labelSize, bodySize);
                yL = drawPageTaxField(cs, leftX, yL, "Purchaser's PAN", sale.customerPan(), labelSize, bodySize);

                float yR = yMetaStart;
                yR = drawPageTaxField(cs, rightX, yR, "Fiscal Year", sale.fiscalYear(), labelSize, bodySize);
                String txnDate = transactionDate(sale);
                yR = drawPageTaxField(cs, rightX, yR, "Transactions Date", txnDate, labelSize, bodySize);
                yR = drawPageTaxField(cs, rightX, yR, "Invoice Issue Date", txnDate, labelSize, bodySize);
                yR = drawPageTaxField(cs, rightX, yR, "Print Count", String.valueOf(printNumber), labelSize, bodySize);
                yR = drawPageTaxField(cs, rightX, yR, "Payment Status", enumLabel(sale.paymentStatus()), labelSize, bodySize);

                y = Math.min(yL, yR) - sectionGap / 2;

                // 4. Method of payment line
                String payMethod = enumLabel(sale.paymentMethod());
                String payLabel = "Method of payment: ";
                PosText.drawAt(cs, FONT_BOLD, labelSize, payLabel, margin, y);
                PosText.drawAt(cs, FONT_REGULAR, bodySize, payMethod,
                        margin + PosText.widthOf(FONT_BOLD, labelSize, payLabel), y);
                y -= lineHeight + sectionGap / 2;

                // 5. Boxed Item Table
                y = drawTaxTableTop(cs, y, xs);
                y = drawPageGridRow(cs, y, xs, headerCells, colAlign, FONT_BOLD, bodySize, lineDrop, cellPad);

                int summaryRows = vatRegistered ? 4 : 2;
                float summaryHeight = summaryRows * (lineDrop + cellPad * 2);
                float wordsHeight = smallSize * 2 + 8;
                float sigHeight = 35f * scale;
                float footerHeight = margin + 25f * scale;
                float bottomReserve = summaryHeight + wordsHeight + sigHeight + footerHeight;

                for (int i = 0; i < items.size(); i++) {
                    SaleItemResponse item = items.get(i);
                    String name = item.productName() != null ? item.productName() : "\u2014";
                    float detailWidth = xs[2] - xs[1] - 2 * cellPad;
                    List<String> nameLines = PosText.wrap(FONT_REGULAR, bodySize, name, detailWidth);
                    if (nameLines.isEmpty()) nameLines = List.of(name);

                    float itemRowHeight = Math.max(1, nameLines.size()) * lineDrop + cellPad * 2;
                    if (y - itemRowHeight < margin + 40f * scale) {
                        cs.close();
                        PDPage next = new PDPage(pageSize);
                        doc.addPage(next);
                        cs = new PDPageContentStream(doc, next);
                        y = pageHeight - margin;
                        PosText.drawCentered(cs, FONT_BOLD, subtitleSize,
                                title + " (Cont.) - Bill #" + sale.invoiceNumber(), y, margin, contentWidth);
                        y -= subtitleSize + sectionGap;
                        y = drawTaxTableTop(cs, y, xs);
                        y = drawPageGridRow(cs, y, xs, headerCells, colAlign, FONT_BOLD, bodySize, lineDrop, cellPad);
                    }

                    BigDecimal rate = figures.rates().get(i);

                    String[][] itemCells = {
                            {String.valueOf(i + 1)},
                            nameLines.toArray(new String[0]),
                            {quantityWithUnit(item)},
                            {money(rate)},
                            {money(figures.lineTotals().get(i))}
                    };
                    y = drawPageGridRow(cs, y, xs, itemCells, colAlign, FONT_REGULAR, bodySize, lineDrop, cellPad);
                }

                if (y - bottomReserve < margin) {
                    cs.close();
                    PDPage next = new PDPage(pageSize);
                    doc.addPage(next);
                    cs = new PDPageContentStream(doc, next);
                    y = pageHeight - margin;
                    PosText.drawCentered(cs, FONT_BOLD, subtitleSize,
                            title + " (Totals) - Bill #" + sale.invoiceNumber(), y, margin, contentWidth);
                    y -= subtitleSize + sectionGap;
                    y = drawTaxTableTop(cs, y, xs);
                }

                // 6. Summary Rows in Boxed Table
                float[] summaryXs = {xs[0], xs[4], xs[5]};
                boolean[] summaryAlign = {true, true};

                y = drawPageGridRow(cs, y, summaryXs, new String[][]{
                        {"Discount"},
                        {money(figures.discount())}
                }, summaryAlign, FONT_REGULAR, bodySize, lineDrop, cellPad);

                if (vatRegistered) {
                    y = drawPageGridRow(cs, y, summaryXs, new String[][]{
                            {"Taxable Amount"},
                            {money(sale.taxableAmount())}
                    }, summaryAlign, FONT_REGULAR, bodySize, lineDrop, cellPad);

                    y = drawPageGridRow(cs, y, summaryXs, new String[][]{
                            {"VAT 13 %"},
                            {money(sale.vatAmount())}
                    }, summaryAlign, FONT_REGULAR, bodySize, lineDrop, cellPad);
                }

                y = drawPageGridRow(cs, y, summaryXs, new String[][]{
                        {"Total"},
                        {money(sale.netTotal())}
                }, summaryAlign, FONT_BOLD, bodySize, lineDrop, cellPad);

                y -= sectionGap;

                // 7. Amount in Words
                String words = "( In words : " + amountInWords(sale.netTotal()) + " )";
                for (String wLine : PosText.wrap(FONT_REGULAR, smallSize, words, contentWidth)) {
                    PosText.drawAt(cs, FONT_REGULAR, smallSize, wLine, margin, y);
                    y -= smallSize + 3;
                }

                y -= sectionGap;

                // 8. Authorized Signature
                float sigWidth = Math.min(contentWidth * 0.35f, 130f * scale);
                float sigRight = contentRight;
                float sigLeft = sigRight - sigWidth;
                float sigLineY = y - 10f * scale;

                cs.setLineWidth(DIVIDER_THICKNESS);
                cs.moveTo(sigLeft, sigLineY);
                cs.lineTo(sigRight, sigLineY);
                cs.stroke();

                PosText.drawCentered(cs, FONT_BOLD, smallSize, "Authorized Signature",
                        sigLineY - (smallSize + 3), sigLeft, sigWidth);

                // 9. Standard Footer
                drawPageInvoiceFooter(cs, margin, contentWidth, contentRight, smallSize);
            } finally {
                cs.close();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate responsive tax invoice PDF for #{}", sale.invoiceNumber(), e);
            throw new PosPrintException("Could not generate tax invoice", e);
        }
    }

    private float drawPageTaxField(PDPageContentStream cs, float x, float y,
                                   String label, String value, float labelSize, float valueSize)
            throws IOException {
        String key = label + ": ";
        PosText.drawAt(cs, FONT_BOLD, labelSize, key, x, y);
        String shown = notBlank(value) ? value : "\u2014";
        PosText.drawAt(cs, FONT_REGULAR, valueSize, shown,
                x + PosText.widthOf(FONT_BOLD, labelSize, key), y);
        return y - (valueSize + 4f);
    }

    private void drawPageInvoiceFooter(PDPageContentStream cs, float margin, float contentWidth,
                                       float contentRight, float smallSize) throws IOException {
        float y = margin;
        cs.setLineWidth(DIVIDER_THICKNESS);
        cs.moveTo(margin, y + smallSize + 6);
        cs.lineTo(contentRight, y + smallSize + 6);
        cs.stroke();

        PosText.drawAt(cs, FONT_REGULAR, smallSize,
                "Generated: " + DATETIME_FMT.format(Instant.now()), margin, y);
    }

    private float drawPageGridRow(PDPageContentStream cs, float yTop, float[] xs,
                                  String[][] cells, boolean[] rightAlign,
                                  PDType1Font font, float size, float lineDrop, float cellPad) throws IOException {
        int maxLines = 1;
        for (String[] cell : cells) {
            maxLines = Math.max(maxLines, cell.length);
        }
        float yBottom = yTop - (maxLines * lineDrop + cellPad * 2);

        for (int i = 0; i < cells.length; i++) {
            float textY = yTop - lineDrop - cellPad + (lineDrop - size) * 0.5f;
            for (String line : cells[i]) {
                if (rightAlign[i]) {
                    PosText.drawRightAligned(cs, font, size, line, xs[i + 1] - cellPad, textY);
                } else {
                    PosText.drawAt(cs, font, size, line, xs[i] + cellPad, textY);
                }
                textY -= lineDrop;
            }
        }

        cs.setLineWidth(DIVIDER_THICKNESS);
        cs.moveTo(xs[0], yBottom);
        cs.lineTo(xs[xs.length - 1], yBottom);
        cs.stroke();
        for (float x : xs) {
            cs.moveTo(x, yTop);
            cs.lineTo(x, yBottom);
            cs.stroke();
        }
        return yBottom;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // IRD Sales Book (A4 Landscape, 10 columns)
    // ═══════════════════════════════════════════════════════════════════════

    public byte[] generateSalesBook(SalesBookResponse book) {
        log.debug("Generating sales book PDF for {} ({} bills)", book.firmName(), book.rows().size());

        float[] xs = new float[BOOK_COL_FRACTIONS.length];
        for (int i = 0; i < xs.length; i++) {
            xs[i] = MARGIN + LANDSCAPE_CONTENT_WIDTH * BOOK_COL_FRACTIONS[i];
        }
        float[] bandXs = {xs[0], xs[4], xs[8], xs[10]};
        float[] totalXs = {xs[0], xs[4], xs[5], xs[6], xs[7], xs[8], xs[9], xs[10]};

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(A4_LANDSCAPE);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            try {
                float y = drawSalesBookHeading(cs, book, xs);

                for (SalesBookRowResponse row : book.rows()) {
                    if (y - LINE_HEIGHT * 2 < MARGIN + A4_FOOTER_RESERVE) {
                        cs.close();
                        PDPage next = new PDPage(A4_LANDSCAPE);
                        doc.addPage(next);
                        cs = new PDPageContentStream(doc, next);
                        y = drawSalesBookTableHead(cs, LANDSCAPE_HEIGHT - MARGIN, xs, bandXs);
                    }
                    y = drawTaxGridRow(cs, y, xs, new String[][]{
                            {truncate(row.date(), 12)},
                            {truncate(row.billNumber(), 14)},
                            {truncate(row.buyerName(), 24)},
                            {truncate(row.buyerPan(), 18)},
                            {money(row.totalSales())},
                            {money(row.nonTaxableSales())},
                            {money(row.exportSales())},
                            {money(row.discount())},
                            {money(row.taxableAmount())},
                            {money(row.tax())}
                    }, BOOK_ROW_ALIGN, FONT_REGULAR, SIZE_SMALL);
                }

                SalesBookTotalResponse total = book.total();
                drawTaxGridRow(cs, y, totalXs, new String[][]{
                        {"Total amount"},
                        {money(total.totalSales())},
                        {money(total.nonTaxableSales())},
                        {money(total.exportSales())},
                        {money(total.discount())},
                        {money(total.taxableAmount())},
                        {money(total.tax())}
                }, new boolean[]{false, true, true, true, true, true, true}, FONT_BOLD, SIZE_SMALL);

                drawLandscapeFooter(cs);
            } finally {
                cs.close();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate sales book PDF for {}", book.firmName(), e);
            throw new PosPrintException("Could not generate sales book", e);
        }
    }

    private float drawSalesBookHeading(PDPageContentStream cs, SalesBookResponse book, float[] xs)
            throws IOException {
        float y = LANDSCAPE_HEIGHT - MARGIN;

        PosText.drawCentered(cs, FONT_BOLD, SIZE_SUBTITLE, "Sales Book", y, MARGIN, LANDSCAPE_CONTENT_WIDTH);
        y -= (SIZE_SUBTITLE + 6);

        y = drawTaxField(cs, MARGIN, y, "Name of Firm", book.firmName());
        y = drawTaxField(cs, MARGIN, y, "PAN", book.pan());
        y = drawTaxField(cs, MARGIN, y, "Duration of Sales", resolveBookDuration(book));
        y -= SECTION_GAP / 2;

        return drawSalesBookTableHead(cs, y, xs, new float[]{xs[0], xs[4], xs[8], xs[10]});
    }

    private String resolveBookDuration(SalesBookResponse book) {
        return notBlank(book.duration()) ? book.duration() : "\u2014";
    }

    private float drawSalesBookTableHead(PDPageContentStream cs, float y, float[] xs, float[] bandXs)
            throws IOException {
        y = drawTaxTableTop(cs, y, xs);
        y = drawSalesBookBand(cs, y, bandXs);
        return drawTaxGridRow(cs, y, xs, BOOK_HEADER, BOOK_HEADER_ALIGN, FONT_BOLD, SIZE_SMALL);
    }

    private float drawSalesBookBand(PDPageContentStream cs, float yTop, float[] xs) throws IOException {
        String[] captions = {"Invoice", "", "Taxable Sales"};
        float lineDrop = SIZE_SMALL + 4;
        float yBottom = yTop - (lineDrop + 4);

        for (int i = 0; i < captions.length; i++) {
            if (captions[i].isEmpty()) continue;
            PosText.drawCentered(cs, FONT_BOLD, SIZE_SMALL, captions[i], yTop - lineDrop,
                    xs[i], xs[i + 1] - xs[i]);
        }

        cs.setLineWidth(DIVIDER_THICKNESS);
        cs.moveTo(xs[0], yBottom);
        cs.lineTo(xs[xs.length - 1], yBottom);
        cs.stroke();
        for (float x : xs) {
            cs.moveTo(x, yTop);
            cs.lineTo(x, yBottom);
            cs.stroke();
        }
        return yBottom;
    }

    private float drawTaxTableTop(PDPageContentStream cs, float y, float[] xs) throws IOException {
        cs.setLineWidth(DIVIDER_THICKNESS);
        cs.moveTo(xs[0], y);
        cs.lineTo(xs[xs.length - 1], y);
        cs.stroke();
        return y;
    }

    private float drawTaxGridRow(PDPageContentStream cs, float yTop, float[] xs,
                                 String[][] cells, boolean[] rightAlign,
                                 PDType1Font font, float size) throws IOException {
        int maxLines = 1;
        for (String[] cell : cells) {
            maxLines = Math.max(maxLines, cell.length);
        }
        float lineDrop = size + 4;
        float yBottom = yTop - (maxLines * lineDrop + 4);

        for (int i = 0; i < cells.length; i++) {
            float textY = yTop - lineDrop;
            for (String line : cells[i]) {
                if (rightAlign[i]) {
                    PosText.drawRightAligned(cs, font, size, line, xs[i + 1] - 4, textY);
                } else {
                    PosText.drawAt(cs, font, size, line, xs[i] + 4, textY);
                }
                textY -= lineDrop;
            }
        }

        cs.setLineWidth(DIVIDER_THICKNESS);
        cs.moveTo(xs[0], yBottom);
        cs.lineTo(xs[xs.length - 1], yBottom);
        cs.stroke();
        for (float x : xs) {
            cs.moveTo(x, yTop);
            cs.lineTo(x, yBottom);
            cs.stroke();
        }
        return yBottom;
    }

    private float drawTaxField(PDPageContentStream cs, float x, float y,
                               String label, String value) throws IOException {
        String key = label + ": ";
        PosText.drawAt(cs, FONT_BOLD, SIZE_BODY, key, x, y);
        String shown = notBlank(value) ? value : "\u2014";
        PosText.drawAt(cs, FONT_REGULAR, SIZE_BODY, shown,
                x + PosText.widthOf(FONT_BOLD, SIZE_BODY, key), y);
        return y - LINE_HEIGHT;
    }

    private void drawLandscapeFooter(PDPageContentStream cs) throws IOException {
        float y = MARGIN - 10;
        cs.setLineWidth(DIVIDER_THICKNESS);
        cs.moveTo(MARGIN, y + LINE_HEIGHT + 4);
        cs.lineTo(MARGIN + LANDSCAPE_CONTENT_WIDTH, y + LINE_HEIGHT + 4);
        cs.stroke();

        PosText.drawAt(cs, FONT_REGULAR, SIZE_SMALL,
                "Generated: " + DATETIME_FMT.format(Instant.now()), MARGIN, y + 2);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Thermal Invoice (POS roll)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Roll length the invoice needs. Mirrors the drawing order of {@link #renderReceipt}
     * term for term, over the very same pre-wrapped content, so the measured page and the
     * printed one can never drift apart.
     */
    private float posInvoiceHeight(PosHeaderContent header, List<String> stampLines,
                                   List<List<String>> metaBlocks, List<List<PosRun>> paymentLines,
                                   List<PosItemBlock> blocks, int summaryRows, List<String> wordsLines,
                                   List<String> footerLines, PosLayout layout) {
        float height = layout.margin() * 2; // top and bottom margin

        height += posHeaderHeight(header, layout);

        height += stampLines.size() * (layout.subtitleSize() + 3);
        if (!stampLines.isEmpty()) {
            height += layout.sectionGap() / 2;
        }

        for (List<String> metaBlock : metaBlocks) {
            if (metaBlock.isEmpty()) continue;
            height += metaBlock.size() * layout.lineHeight() + layout.sectionGap() / 2;
        }

        height += paymentLines.size() * layout.lineHeight() + layout.sectionGap() / 2;

        // Items
        height += layout.dividerGap(); // divider
        height += layout.lineHeight(); // column header
        height += layout.dividerGap(); // divider
        for (PosItemBlock block : blocks) {
            height += block.height(layout);
        }
        height += layout.dividerGap() + layout.sectionGap() / 2; // divider above the totals
        height += summaryRows * layout.lineHeight();

        height += layout.sectionGap() / 2 + wordsLines.size() * (layout.smallSize() + 3);
        height += layout.sectionGap();
        height += posSignatureHeight(layout);
        height += posFooterHeight(footerLines, layout);

        return height;
    }

    /**
     * Header lines, already word-wrapped to the content column. Drawn as single centered
     * strings, a company name wider than the roll started left of the margin and ran past
     * the right edge, so the print head clipped both ends.
     */
    private record PosHeaderContent(List<String> nameLines, List<String> contactLines,
                                    List<String> panLines, List<String> titleLines) {
    }

    private static PosHeaderContent posHeaderContent(MartBranding branding, String title, PosLayout layout) {
        String companyName = upper(branding.companyName());
        List<String> nameLines = PosText.wrap(FONT_BOLD, layout.titleSize(), companyName, layout.contentWidth());
        if (nameLines.isEmpty()) nameLines = List.of(companyName);

        List<String> contactLines =
                PosText.wrap(FONT_REGULAR, layout.smallSize(), buildContactLine(branding), layout.contentWidth());

        List<String> panLines = notBlank(branding.registrationNumber())
                ? PosText.wrap(FONT_REGULAR, layout.smallSize(),
                        "PAN: " + branding.registrationNumber(), layout.contentWidth())
                : List.of();

        List<String> titleLines = PosText.wrap(FONT_BOLD, layout.subtitleSize(), title, layout.contentWidth());
        if (titleLines.isEmpty()) titleLines = List.of("");

        return new PosHeaderContent(nameLines, contactLines, panLines, titleLines);
    }

    /**
     * Exact height contributed by {@link #drawPosHeader}: company name, one combined
     * Tel/Email line, PAN line, divider, title, divider.
     */
    private float posHeaderHeight(PosHeaderContent header, PosLayout layout) {
        float h = layout.titleSize() * ASCENT_RATIO; // ascender clearance for the first line
        h += header.nameLines().size() * (layout.titleSize() + 4);
        h += header.contactLines().size() * (layout.smallSize() + 3);
        h += header.panLines().size() * (layout.smallSize() + 3);
        h += 3f; // buffer before divider
        h += layout.dividerGap(); // divider
        h += header.titleLines().size() * (layout.subtitleSize() + layout.sectionGap()); // title
        h += layout.dividerGap(); // divider
        return h;
    }

    private float drawPosHeader(PDPageContentStream cs, float y, PosHeaderContent header,
                                PosLayout layout) throws IOException {
        // The caller hands in the top margin as a baseline, but glyphs rise above their
        // baseline — without this drop the company name is shaved off along the top edge.
        y -= layout.titleSize() * ASCENT_RATIO;

        for (String line : header.nameLines()) {
            PosText.drawCentered(cs, FONT_BOLD, layout.titleSize(), line, y, layout.margin(), layout.contentWidth());
            y -= layout.titleSize() + 4;
        }
        for (String line : header.contactLines()) {
            PosText.drawCentered(cs, FONT_REGULAR, layout.smallSize(), line, y, layout.margin(), layout.contentWidth());
            y -= layout.smallSize() + 3;
        }
        for (String line : header.panLines()) {
            PosText.drawCentered(cs, FONT_REGULAR, layout.smallSize(), line, y, layout.margin(), layout.contentWidth());
            y -= layout.smallSize() + 3;
        }
        y -= 3f; // small buffer before divider — must match posHeaderHeight()

        y = drawPosDivider(cs, y, layout);

        for (String line : header.titleLines()) {
            PosText.drawCentered(cs, FONT_BOLD, layout.subtitleSize(), line, y, layout.margin(), layout.contentWidth());
            y -= layout.subtitleSize() + layout.sectionGap();
        }

        return drawPosDivider(cs, y, layout);
    }

    /**
     * One line of the bill as it fits a roll: serial number and details on the first
     * line(s), quantity and per-unit amount below them, the line total right-aligned beside
     * the quantity, and the line's own discount under that when it has one.
     */
    private record PosItemBlock(List<String> nameLines, String qtyPriceLine, String totalText,
                                List<String> extraLines) {
        float height(PosLayout layout) {
            return nameLines.size() * layout.lineHeight()
                    + layout.lineHeight() // qty/price + total line
                    + extraLines.size() * layout.extraLineHeight();
        }
    }

    private List<PosItemBlock> buildPosItemBlocks(List<SaleItemResponse> items, PrintedFigures figures,
                                                  PosLayout layout) {
        float extraWidth = layout.contentWidth() - layout.extraIndent();

        List<PosItemBlock> blocks = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            SaleItemResponse item = items.get(i);
            String name = (i + 1) + ". " + (notBlank(item.productName()) ? item.productName() : "—");
            List<String> nameLines = PosText.wrap(FONT_BOLD, layout.bodySize(), name, layout.contentWidth());
            if (nameLines.isEmpty()) nameLines = List.of(name);

            // A line total is quantity x rate less the line's discount, so the discount
            // prints under it or the block would not add up.
            List<String> extras = isPositive(item.discountAmount())
                    ? PosText.wrap(FONT_REGULAR, layout.smallSize(),
                            "Less discount " + money(figures.lineDiscounts().get(i)), extraWidth)
                    : List.of();

            blocks.add(new PosItemBlock(nameLines,
                    quantityWithUnit(item) + " x " + money(figures.rates().get(i)),
                    money(figures.lineTotals().get(i)),
                    extras));
        }
        return blocks;
    }

    private float drawPosItemBlock(PDPageContentStream cs, float y, PosItemBlock block,
                                   PosLayout layout) throws IOException {
        for (String line : block.nameLines()) {
            PosText.drawAt(cs, FONT_BOLD, layout.bodySize(), line, layout.margin(), y);
            y -= layout.lineHeight();
        }

        PosText.drawAt(cs, FONT_REGULAR, layout.bodySize(), block.qtyPriceLine(),
                layout.margin() + layout.extraIndent(), y);
        PosText.drawRightAligned(cs, FONT_BOLD, layout.bodySize(), block.totalText(),
                layout.contentRight(), y);
        y -= layout.lineHeight();

        for (String extra : block.extraLines()) {
            PosText.drawAt(cs, FONT_REGULAR, layout.smallSize(), extra,
                    layout.margin() + layout.extraIndent(), y);
            y -= layout.extraLineHeight();
        }
        return y;
    }

    private float drawPosItemsHeader(PDPageContentStream cs, float y, PosLayout layout) throws IOException {
        PosText.drawAt(cs, FONT_BOLD, layout.bodySize(), "S.No. Details", layout.margin(), y);
        PosText.drawRightAligned(cs, FONT_BOLD, layout.bodySize(), "Amount (Rs)", layout.contentRight(), y);
        return y - layout.lineHeight();
    }

    private float drawPosDivider(PDPageContentStream cs, float y, PosLayout layout) throws IOException {
        return PosText.drawDashedDivider(cs, layout, y, DIVIDER_THICKNESS);
    }

    /**
     * Appends one "Key: value" field, word-wrapped to the content column so a long value
     * never runs off the edge of the roll. An em dash stands in for anything the bill does
     * not carry.
     */
    private void addPosMetaLine(List<String> out, String key, Object value, PosLayout layout) {
        String shown = value != null && notBlank(value.toString()) ? value.toString() : "—";
        String text = key + ": " + shown;
        List<String> wrapped = PosText.wrap(FONT_REGULAR, layout.bodySize(), text, layout.contentWidth());
        out.addAll(wrapped.isEmpty() ? List.of(text) : wrapped);
    }

    private float drawPosMetaLines(PDPageContentStream cs, float y, List<String> lines, PosLayout layout)
            throws IOException {
        for (String line : lines) {
            PosText.drawAt(cs, FONT_REGULAR, layout.bodySize(), line, layout.margin(), y);
            y -= layout.lineHeight();
        }
        return y;
    }

    /**
     * A run of text together with the font it is measured and drawn in.
     */
    private record PosRun(PDType1Font font, String text) {
    }

    /**
     * The form's payment line — the label in regular weight, the method in bold. The pair
     * fits one line on a roll for every known method, but the method drops to a line of
     * its own if it ever does not.
     */
    private List<List<PosRun>> posPaymentMethodLines(Enum<?> method, PosLayout layout) {
        String label = "Method of payment: ";
        String shown = enumLabel(method);

        float width = PosText.widthOf(FONT_REGULAR, layout.bodySize(), label)
                + PosText.widthOf(FONT_BOLD, layout.bodySize(), shown);
        if (width <= layout.contentWidth()) {
            return List.of(List.of(new PosRun(FONT_REGULAR, label), new PosRun(FONT_BOLD, shown)));
        }
        return List.of(
                List.of(new PosRun(FONT_REGULAR, "Method of payment:")),
                List.of(new PosRun(FONT_BOLD, shown)));
    }

    /**
     * Draws pre-flowed lines of mixed-font runs from the left margin, at body size.
     */
    private float drawPosRunLines(PDPageContentStream cs, float y, List<List<PosRun>> lines,
                                  PosLayout layout) throws IOException {
        for (List<PosRun> line : lines) {
            float x = layout.margin();
            for (PosRun run : line) {
                PosText.drawAt(cs, run.font(), layout.bodySize(), run.text(), x, y);
                x += PosText.widthOf(run.font(), layout.bodySize(), run.text());
            }
            y -= layout.lineHeight();
        }
        return y;
    }

    private float drawPosSummaryRow(PDPageContentStream cs, float y, String label, String amount,
                                    boolean highlight, PosLayout layout) throws IOException {
        PDType1Font font = highlight ? FONT_BOLD : FONT_REGULAR;
        float size = highlight ? layout.bodySize() + 1 : layout.bodySize();

        // The amount owns the right edge; the label is pushed left of it if it would
        // otherwise overlap, so neither can spill outside the content column.
        float amountWidth = PosText.widthOf(font, size, amount);
        float labelWidth = PosText.widthOf(font, size, label);
        float labelX = Math.min(
                layout.margin() + layout.contentWidth() * 0.30f,
                Math.max(layout.margin(), layout.contentRight() - amountWidth - 6f - labelWidth));

        PosText.drawAt(cs, font, size, label, labelX, y);
        PosText.drawRightAligned(cs, font, size, amount, layout.contentRight(), y);

        return y - layout.lineHeight();
    }

    /**
     * Rule to sign on with its caption underneath, right-aligned the way the form has it.
     * Two blank lines are left above the rule so there is room to actually sign the roll.
     */
    private float drawPosSignature(PDPageContentStream cs, float y, PosLayout layout) throws IOException {
        y -= layout.lineHeight() * 2;

        String caption = "Authorized Signature";
        float ruleWidth = Math.min(layout.contentWidth(),
                PosText.widthOf(FONT_BOLD, layout.bodySize(), caption) + layout.extraIndent());
        cs.setLineWidth(DIVIDER_THICKNESS);
        cs.moveTo(layout.contentRight() - ruleWidth, y);
        cs.lineTo(layout.contentRight(), y);
        cs.stroke();
        y -= layout.dividerGap();

        PosText.drawRightAligned(cs, FONT_BOLD, layout.bodySize(), caption, layout.contentRight(), y);
        return y - layout.lineHeight();
    }

    /**
     * Exact height contributed by {@link #drawPosSignature}.
     */
    private float posSignatureHeight(PosLayout layout) {
        return layout.lineHeight() * 2 + layout.dividerGap() + layout.lineHeight();
    }

    /**
     * Footer message and generation timestamp, wrapped to the content column. Built once
     * and handed to both {@link #posFooterHeight} and {@link #drawPosFooter}, so the space
     * reserved always matches what is drawn.
     */
    private List<String> posFooterLines(String message, PosLayout layout) {
        List<String> lines = new ArrayList<>(
                PosText.wrap(FONT_REGULAR, layout.smallSize(), message, layout.contentWidth()));
        lines.addAll(PosText.wrap(FONT_REGULAR, layout.smallSize(),
                "Generated: " + DATETIME_FMT.format(Instant.now()), layout.contentWidth()));
        return lines;
    }

    private float posFooterHeight(List<String> footerLines, PosLayout layout) {
        return layout.sectionGap()
                + layout.dividerGap()
                + footerLines.size() * (layout.smallSize() + 3);
    }

    private void drawPosFooter(PDPageContentStream cs, List<String> footerLines, float y,
                               PosLayout layout) throws IOException {
        y -= layout.sectionGap();
        y = PosText.drawSolidDivider(cs, layout, y, DIVIDER_THICKNESS);

        for (String line : footerLines) {
            PosText.drawCentered(cs, FONT_REGULAR, layout.smallSize(), line, y,
                    layout.margin(), layout.contentWidth());
            y -= layout.smallSize() + 3;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Amount in Words (Nepali System)
    // ═══════════════════════════════════════════════════════════════════════

    private static final String[] WORD_UNITS = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
            "Eighteen", "Nineteen"
    };
    private static final String[] WORD_TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private String amountInWords(BigDecimal value) {
        BigDecimal amount = (value == null ? BigDecimal.ZERO : value).abs().setScale(2, RoundingMode.HALF_UP);
        long rupees = amount.longValue();
        int paisa = amount.subtract(BigDecimal.valueOf(rupees)).movePointRight(2).intValue();

        StringBuilder sb = new StringBuilder(wordsForNumber(rupees)).append(" Rupees");
        if (paisa > 0) {
            sb.append(" and ").append(wordsForNumber(paisa)).append(" Paisa");
        }
        return sb.append(" Only").toString();
    }

    private String wordsForNumber(long number) {
        if (number == 0) return "Zero";

        long rest = number;
        long crore = rest / 10_000_000;
        rest %= 10_000_000;
        long lakh = rest / 100_000;
        rest %= 100_000;
        long thousand = rest / 1_000;
        rest %= 1_000;
        long hundred = rest / 100;
        rest %= 100;

        StringBuilder sb = new StringBuilder();
        if (crore > 0) sb.append(wordsForNumber(crore)).append(" Crore ");
        if (lakh > 0) sb.append(wordsBelowHundred(lakh)).append(" Lakh ");
        if (thousand > 0) sb.append(wordsBelowHundred(thousand)).append(" Thousand ");
        if (hundred > 0) sb.append(wordsBelowHundred(hundred)).append(" Hundred ");
        if (rest > 0) sb.append(wordsBelowHundred(rest)).append(' ');
        return sb.toString().trim();
    }

    private String wordsBelowHundred(long number) {
        if (number < 20) return WORD_UNITS[(int) number];
        return (WORD_TENS[(int) (number / 10)] + " " + WORD_UNITS[(int) (number % 10)]).trim();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private static String buildContactLine(MartBranding branding) {
        List<String> parts = new ArrayList<>();
        if (notBlank(branding.companyPhone())) parts.add("Tel: " + branding.companyPhone());
        if (notBlank(branding.email())) parts.add("Email: " + branding.email());
        return String.join(", ", parts);
    }

    private static String formatInstant(Instant instant) {
        return instant == null ? "\u2014" : DATE_FMT.format(instant);
    }

    /**
     * A bill that has never been printed still prints as print no. 1 — the original — so
     * the field is never blank and the copies that follow it count up from there.
     */
    private static int printNumber(SaleDetailResponse sale) {
        return Math.max(1, sale.printCount() != null ? sale.printCount() : 1);
    }

    /**
     * The bill is a Nepali document, so it carries the BS date entered at the till; without
     * one it falls back to the AD date the sale was made.
     */
    private static String transactionDate(SaleDetailResponse sale) {
        return notBlank(sale.nepaliDate()) ? sale.nepaliDate() : formatInstant(sale.soldAt());
    }

    private static String quantityWithUnit(SaleItemResponse item) {
        return quantity(item.quantity()) + (notBlank(item.unitSymbol()) ? " " + item.unitSymbol() : "");
    }

    /**
     * An enum as the form spells it — CASH prints as "Cash" — so the printed bill reads as a
     * word rather than as the constant behind it.
     */
    private static String enumLabel(Enum<?> value) {
        if (value == null) return "\u2014";
        StringBuilder label = new StringBuilder();
        for (String word : value.name().split("_")) {
            if (word.isEmpty()) continue;
            if (!label.isEmpty()) label.append(' ');
            label.append(word.charAt(0)).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return label.toString();
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "\u2014";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 1) + "\u2026";
    }

    private static String money(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String quantity(BigDecimal amount) {
        if (amount == null) return "0";
        BigDecimal stripped = amount.stripTrailingZeros();
        return (stripped.scale() < 0 ? stripped.setScale(0, RoundingMode.UNNECESSARY) : stripped)
                .toPlainString();
    }

    private static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String upper(String value) {
        return notBlank(value) ? value.toUpperCase() : "MART";
    }
}
