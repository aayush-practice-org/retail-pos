package io.aygh.billing.service;

import io.aygh.sales.dto.response.SaleDetailResponse;
import io.aygh.sales.dto.response.SaleItemResponse;
import io.aygh.sales.dto.response.SalesBookResponse;
import io.aygh.sales.dto.response.SalesBookRowResponse;
import io.aygh.sales.dto.response.SalesBookTotalResponse;
import io.aygh.shared.entity.PaymentStatus;
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

/**
 * Renders sales as printable IRD invoices (responsive A4/A5/A6 Schedule 5 & POS thermal roll)
 * and generates the IRD Sales Book (A4 landscape).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfService {

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
     * Renders the tax invoice according to the requested paper type (MM80, MM75, A4, A5, A6).
     */
    public byte[] renderTaxInvoice(SaleDetailResponse sale, PrintPaperType paperType) {
        PrintPaperType type = paperType != null ? paperType : PrintPaperType.MM80;
        if (type.isPos()) {
            return renderReceipt(sale, type.toPosPaper());
        }
        return renderPageTaxInvoice(sale, type);
    }

    /**
     * The filed copy: A4, itemised, with the mart's registration details.
     */
    public byte[] renderA4(SaleDetailResponse sale) {
        return renderPageTaxInvoice(sale, PrintPaperType.A4);
    }

    /**
     * The counter copy, on whatever roll the till feeds.
     */
    public byte[] renderReceipt(SaleDetailResponse sale, PosPaper paper) {
        MartBranding branding = brandingService.resolve();
        PosLayout layout = PosLayout.of(paper);
        log.debug("Rendering {} receipt for {}", paper, sale.invoiceNumber());

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            drawReceipt(document, sale, branding, layout);
            document.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new PosPrintException("Could not render receipt for " + sale.invoiceNumber(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Responsive Page Invoice (A4, A5, A6 - Schedule 5 Boxed Table Layout)
    // ═══════════════════════════════════════════════════════════════════════

    private byte[] renderPageTaxInvoice(SaleDetailResponse sale, PrintPaperType paperType) {
        boolean vatRegistered = sale.taxScheme() == TaxScheme.VAT;
        String title = vatRegistered ? "Tax Invoice" : "Invoice";
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
        BigDecimal discount = sale.discountAmount() != null ? sale.discountAmount() : BigDecimal.ZERO;

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
                String txnDate = notBlank(sale.nepaliDate()) ? sale.nepaliDate() : formatInstant(sale.soldAt());
                yR = drawPageTaxField(cs, rightX, yR, "Transactions Date", txnDate, labelSize, bodySize);
                yR = drawPageTaxField(cs, rightX, yR, "Invoice Issue Date", txnDate, labelSize, bodySize);
                yR = drawPageTaxField(cs, rightX, yR, "Payment Status", String.valueOf(sale.paymentStatus()), labelSize, bodySize);

                y = Math.min(yL, yR) - sectionGap / 2;

                // 4. Method of payment line
                String payMethod = String.valueOf(sale.paymentMethod());
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

                    BigDecimal rate = item.rate() != null ? item.rate() : BigDecimal.ZERO;
                    String qtyStr = quantity(item.quantity()) + (notBlank(item.unitSymbol()) ? " " + item.unitSymbol() : "");

                    String[][] itemCells = {
                            {String.valueOf(i + 1)},
                            nameLines.toArray(new String[0]),
                            {qtyStr},
                            {money(rate)},
                            {money(item.lineTotal())}
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
                        {money(discount)}
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
        if (notBlank(book.duration())) {
            return book.duration();
        }
        boolean hasMonth = notBlank(book.month());
        boolean hasYear = notBlank(book.year());
        if (hasMonth && hasYear) {
            return "Month " + book.month() + "   Year " + book.year();
        }
        if (hasMonth) {
            return "Month " + book.month();
        }
        if (hasYear) {
            return "Year " + book.year();
        }
        return "\u2014";
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
    // Thermal Receipt (POS)
    // ═══════════════════════════════════════════════════════════════════════

    private record ItemBlock(List<String> nameLines, String quantityLine, String amount, String discountLine) {
        int lineCount() {
            return nameLines.size() + 1 + (discountLine == null ? 0 : 1);
        }
    }

    private void drawReceipt(PDDocument document, SaleDetailResponse sale,
                             MartBranding branding, PosLayout layout) throws IOException {

        List<ItemBlock> blocks = buildItemBlocks(sale, layout);
        float height = measureReceipt(sale, branding, layout, blocks);

        PDPage page = new PDPage(new PDRectangle(layout.width(), height));
        document.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
            float y = height - layout.margin();
            y = drawReceiptHeader(cs, branding, sale, layout, y);
            y = drawReceiptItems(cs, layout, blocks, y);
            y = drawReceiptTotals(cs, sale, layout, y);
            drawReceiptFooter(cs, sale, layout, y);
        }
    }

    private float measureReceipt(SaleDetailResponse sale, MartBranding branding,
                                 PosLayout layout, List<ItemBlock> blocks) {

        float height = layout.margin() * 2;
        height += receiptHeaderHeight(branding, sale, layout);

        for (ItemBlock block : blocks) {
            height += block.lineCount() * layout.lineHeight();
        }

        height += layout.dividerGap() * 2;
        height += receiptTotalsHeight(sale, layout);
        height += layout.sectionGap() + layout.lineHeight() * 4;

        return height;
    }

    private float receiptHeaderHeight(MartBranding branding, SaleDetailResponse sale, PosLayout layout) {
        float height = layout.lineHeight() * 2;
        if (contactLine(branding) != null) height += layout.lineHeight();
        if (notBlank(branding.registrationNumber())) height += layout.lineHeight();
        height += layout.dividerGap() * 2;
        height += layout.lineHeight() * 2;
        if (notBlank(sale.nepaliDate())) height += layout.lineHeight();
        if (notBlank(sale.customerName())) height += layout.lineHeight();
        if (notBlank(sale.customerPan())) height += layout.lineHeight();
        height += layout.sectionGap();
        return height;
    }

    private float receiptTotalsHeight(SaleDetailResponse sale, PosLayout layout) {
        int lines = 2;
        if (isPositive(sale.discountAmount())) lines += 2;
        if (sale.taxScheme() == TaxScheme.VAT) lines++;
        if (isPositive(sale.paidAmount())) lines++;
        if (isPositive(sale.changeAmount())) lines++;
        if (isPositive(sale.dueAmount())) lines++;
        return lines * layout.lineHeight();
    }

    private List<ItemBlock> buildItemBlocks(SaleDetailResponse sale, PosLayout layout) {
        float amountWidth = PosText.widthOf(FONT_REGULAR, layout.bodySize(), "0000000.00");
        float nameWidth = layout.contentWidth() - amountWidth - 4;

        List<ItemBlock> blocks = new ArrayList<>(sale.items().size());
        for (SaleItemResponse item : sale.items()) {
            List<String> nameLines = PosText.wrap(FONT_BOLD, layout.bodySize(), item.productName(), nameWidth);
            if (nameLines.isEmpty()) {
                nameLines = List.of("-");
            }

            blocks.add(new ItemBlock(
                    nameLines,
                    quantity(item.quantity()) + " " + item.unitSymbol() + " x " + money(item.rate()),
                    money(item.lineTotal()),
                    isPositive(item.discountAmount()) ? "less disc " + money(item.discountAmount()) : null));
        }
        return blocks;
    }

    private float drawReceiptHeader(PDPageContentStream cs, MartBranding branding,
                                    SaleDetailResponse sale, PosLayout layout, float y) throws IOException {

        PosText.drawCentered(cs, FONT_BOLD, layout.titleSize(),
                upper(branding.companyName()), y, layout.margin(), layout.contentWidth());
        y -= layout.lineHeight();

        String contact = contactLine(branding);
        if (contact != null) {
            PosText.drawCentered(cs, FONT_REGULAR, layout.smallSize(), contact,
                    y, layout.margin(), layout.contentWidth());
            y -= layout.lineHeight();
        }
        if (notBlank(branding.registrationNumber())) {
            PosText.drawCentered(cs, FONT_REGULAR, layout.smallSize(),
                    "PAN/VAT: " + branding.registrationNumber(), y, layout.margin(), layout.contentWidth());
            y -= layout.lineHeight();
        }

        PosText.drawCentered(cs, FONT_BOLD, layout.subtitleSize(), documentTitle(sale),
                y, layout.margin(), layout.contentWidth());
        y -= layout.lineHeight();

        y = PosText.drawDashedDivider(cs, layout, y, 0.5f);

        PosText.drawAt(cs, FONT_REGULAR, layout.bodySize(), sale.invoiceNumber(), layout.margin(), y);
        y -= layout.lineHeight();
        PosText.drawAt(cs, FONT_REGULAR, layout.smallSize(),
                DATETIME_FMT.format(sale.soldAt()), layout.margin(), y);
        PosText.drawRightAligned(cs, FONT_REGULAR, layout.smallSize(),
                String.valueOf(sale.paymentMethod()), layout.contentRight(), y);
        y -= layout.lineHeight();

        if (notBlank(sale.nepaliDate())) {
            String bsLine = "BS: " + sale.nepaliDate()
                    + (notBlank(sale.fiscalYear()) ? "  FY: " + sale.fiscalYear() : "");
            PosText.drawAt(cs, FONT_REGULAR, layout.smallSize(), bsLine, layout.margin(), y);
            y -= layout.lineHeight();
        }

        if (notBlank(sale.customerName())) {
            PosText.drawAt(cs, FONT_REGULAR, layout.smallSize(),
                    "Customer: " + sale.customerName(), layout.margin(), y);
            y -= layout.lineHeight();
        }
        if (notBlank(sale.customerPan())) {
            PosText.drawAt(cs, FONT_REGULAR, layout.smallSize(),
                    "PAN: " + sale.customerPan(), layout.margin(), y);
            y -= layout.lineHeight();
        }

        y -= layout.sectionGap();
        return PosText.drawDashedDivider(cs, layout, y, 0.5f);
    }

    private float drawReceiptItems(PDPageContentStream cs, PosLayout layout,
                                   List<ItemBlock> blocks, float y) throws IOException {

        for (ItemBlock block : blocks) {
            for (String line : block.nameLines()) {
                PosText.drawAt(cs, FONT_BOLD, layout.bodySize(), line, layout.margin(), y);
                y -= layout.lineHeight();
            }

            PosText.drawAt(cs, FONT_REGULAR, layout.smallSize(),
                    block.quantityLine(), layout.margin() + layout.extraIndent(), y);
            PosText.drawRightAligned(cs, FONT_REGULAR, layout.bodySize(),
                    block.amount(), layout.contentRight(), y);
            y -= layout.lineHeight();

            if (block.discountLine() != null) {
                PosText.drawAt(cs, FONT_REGULAR, layout.smallSize(),
                        block.discountLine(), layout.margin() + layout.extraIndent(), y);
                y -= layout.lineHeight();
            }
        }
        return y;
    }

    private float drawReceiptTotals(PDPageContentStream cs, SaleDetailResponse sale,
                                    PosLayout layout, float y) throws IOException {

        y = PosText.drawDashedDivider(cs, layout, y, 0.5f);

        y = receiptTotalLine(cs, layout, "Sub total", money(sale.subTotal()), y, false);
        if (isPositive(sale.discountAmount())) {
            y = receiptTotalLine(cs, layout, "Discount", "- " + money(sale.discountAmount()), y, false);
            y = receiptTotalLine(cs, layout, "Taxable", money(sale.taxableAmount()), y, false);
        }
        if (sale.taxScheme() == TaxScheme.VAT) {
            y = receiptTotalLine(cs, layout, "VAT", money(sale.vatAmount()), y, false);
        }

        y = receiptTotalLine(cs, layout, "TOTAL", money(sale.netTotal()), y, true);

        if (isPositive(sale.paidAmount())) {
            y = receiptTotalLine(cs, layout, "Paid", money(sale.paidAmount()), y, false);
        }
        if (isPositive(sale.changeAmount())) {
            y = receiptTotalLine(cs, layout, "Change", money(sale.changeAmount()), y, false);
        }
        if (isPositive(sale.dueAmount())) {
            y = receiptTotalLine(cs, layout, "Due", money(sale.dueAmount()), y, true);
        }

        return y;
    }

    private float receiptTotalLine(PDPageContentStream cs, PosLayout layout, String label,
                                   String value, float y, boolean bold) throws IOException {
        PDType1Font font = bold ? FONT_BOLD : FONT_REGULAR;
        PosText.drawAt(cs, font, layout.bodySize(), label, layout.margin(), y);
        PosText.drawRightAligned(cs, font, layout.bodySize(), value, layout.contentRight(), y);
        return y - layout.lineHeight();
    }

    private void drawReceiptFooter(PDPageContentStream cs, SaleDetailResponse sale,
                                   PosLayout layout, float y) throws IOException {
        y -= layout.sectionGap();
        String words = "( In words : " + amountInWords(sale.netTotal()) + " )";
        for (String wLine : PosText.wrap(FONT_REGULAR, layout.smallSize(), words, layout.contentWidth())) {
            PosText.drawCentered(cs, FONT_REGULAR, layout.smallSize(), wLine, y, layout.margin(), layout.contentWidth());
            y -= layout.smallSize() + 2;
        }

        y -= 4f;
        PosText.drawCentered(cs, FONT_REGULAR, layout.smallSize(),
                sale.paymentStatus() == PaymentStatus.PAID
                        ? "Thank you, please come again"
                        : "Balance outstanding",
                y, layout.margin(), layout.contentWidth());
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

    private static String documentTitle(SaleDetailResponse sale) {
        return sale.taxScheme() == TaxScheme.VAT ? "TAX INVOICE" : "INVOICE";
    }

    private static String contactLine(MartBranding branding) {
        return join(" · ",
                blankToNull(branding.companyAddress()),
                prefixed("Tel ", branding.companyPhone()));
    }

    private static String buildContactLine(MartBranding branding) {
        List<String> parts = new ArrayList<>();
        if (notBlank(branding.companyPhone())) parts.add("Tel: " + branding.companyPhone());
        if (notBlank(branding.email())) parts.add("Email: " + branding.email());
        return String.join("  |  ", parts);
    }

    private static String formatInstant(Instant instant) {
        return instant == null ? "\u2014" : DATE_FMT.format(instant);
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

    private static String blankToNull(String value) {
        return notBlank(value) ? value : null;
    }

    private static String prefixed(String prefix, String value) {
        return notBlank(value) ? prefix + value : null;
    }

    private static String upper(String value) {
        return notBlank(value) ? value.toUpperCase() : "MART";
    }

    private static String join(String separator, String... parts) {
        List<String> present = new ArrayList<>();
        for (String part : parts) {
            if (notBlank(part)) {
                present.add(part);
            }
        }
        return present.isEmpty() ? null : String.join(separator, present);
    }
}
