package io.aygh.billing.service;

import io.aygh.sales.dto.response.SaleDetailResponse;
import io.aygh.sales.dto.response.SaleItemResponse;
import io.aygh.shared.entity.PaymentStatus;
import io.aygh.shared.entity.TaxScheme;
import io.aygh.shared.print.PosLayout;
import io.aygh.shared.print.PosPaper;
import io.aygh.shared.print.PosPrintException;
import io.aygh.shared.print.PosText;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a sale as a printable invoice — A4 for a VAT bill that gets filed, and
 * a thermal roll for the one handed across the counter.
 * <p>
 * Both start from the same {@link SaleDetailResponse} and the same
 * {@link MartBranding}, so the counter copy and the filed copy can never carry
 * different numbers.
 * <p>
 * The receipt is not pinned to 80mm paper: the caller passes a {@link PosPaper}
 * and every margin, font size and line height is derived from it through
 * {@link PosLayout}, so one set of drawing code serves every roll the mart might
 * feed.
 * <p>
 * Height and layout cannot drift apart, because the wrapped item blocks are
 * built once by {@link #buildItemBlocks} and that same list — plus the same
 * header-height formula {@link #drawReceiptHeader} uses — both measures the page
 * and renders it. Computing a height one way and drawing another is the classic
 * way a receipt ends up with a blank tail or a clipped total.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfService {

    // ── Fonts: Standard Type-1, always present, nothing to embed ──────────
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    // ── A4 geometry ───────────────────────────────────────────────────────
    private static final float A4_MARGIN = 50f;
    private static final float A4_WIDTH = PDRectangle.A4.getWidth();
    private static final float A4_HEIGHT = PDRectangle.A4.getHeight();
    private static final float A4_CONTENT_WIDTH = A4_WIDTH - 2 * A4_MARGIN;
    private static final float A4_FOOTER_RESERVE = 90f;

    private static final float SIZE_TITLE = 18f;
    private static final float SIZE_SUBTITLE = 11f;
    private static final float SIZE_BODY = 9.5f;
    private static final float SIZE_SMALL = 8f;
    private static final float LINE_HEIGHT = 15f;
    private static final float SECTION_GAP = 12f;

    /**
     * The business day every printed timestamp is rendered against.
     */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kathmandu");

    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm").withZone(BUSINESS_ZONE);

    private final MartBrandingService brandingService;

    // ═══════════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * The filed copy: A4, itemised, with the mart's registration details.
     */
    public byte[] renderA4(SaleDetailResponse sale) {
        MartBranding branding = brandingService.resolve();
        log.debug("Rendering A4 invoice for {}", sale.invoiceNumber());

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            drawA4(document, sale, branding);
            document.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new PosPrintException("Could not render invoice " + sale.invoiceNumber(), e);
        }
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
    // A4
    // ═══════════════════════════════════════════════════════════════════════

    private void drawA4(PDDocument document, SaleDetailResponse sale, MartBranding branding)
            throws IOException {

        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(document, page);

        float y = A4_HEIGHT - A4_MARGIN;
        y = drawA4Header(cs, branding, sale, y);
        y = drawA4ItemHeader(cs, y);

        for (SaleItemResponse item : sale.items()) {
            // A page break has to close the stream it was drawing into, or PDFBox
            // writes both pages' content into the first one.
            if (y < A4_MARGIN + A4_FOOTER_RESERVE) {
                cs.close();
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                cs = new PDPageContentStream(document, page);
                y = A4_HEIGHT - A4_MARGIN;
                y = drawA4ItemHeader(cs, y);
            }
            y = drawA4Item(cs, item, y);
        }

        y = drawA4Totals(cs, sale, y);
        drawA4Footer(cs, sale);
        cs.close();
    }

    private float drawA4Header(PDPageContentStream cs, MartBranding branding,
                               SaleDetailResponse sale, float y) throws IOException {

        PosText.drawCentered(cs, FONT_BOLD, SIZE_TITLE,
                upper(branding.companyName()), y, A4_MARGIN, A4_CONTENT_WIDTH);
        y -= LINE_HEIGHT + 2;

        String contact = join(" · ",
                blankToNull(branding.companyAddress()),
                prefixed("Tel ", branding.companyPhone()),
                blankToNull(branding.email()));
        if (contact != null) {
            PosText.drawCentered(cs, FONT_REGULAR, SIZE_SMALL, contact, y, A4_MARGIN, A4_CONTENT_WIDTH);
            y -= LINE_HEIGHT;
        }
        if (notBlank(branding.registrationNumber())) {
            PosText.drawCentered(cs, FONT_REGULAR, SIZE_SMALL,
                    "PAN/VAT: " + branding.registrationNumber(), y, A4_MARGIN, A4_CONTENT_WIDTH);
            y -= LINE_HEIGHT;
        }

        y -= 4;
        PosText.drawCentered(cs, FONT_BOLD, SIZE_SUBTITLE, documentTitle(sale), y, A4_MARGIN, A4_CONTENT_WIDTH);
        y -= SECTION_GAP;
        y = solidRule(cs, y, 1f);

        // ── Invoice meta, two columns ─────────────────────────────────────
        float rightX = A4_MARGIN + A4_CONTENT_WIDTH;
        PosText.drawAt(cs, FONT_BOLD, SIZE_BODY, "Invoice: " + sale.invoiceNumber(), A4_MARGIN, y);
        PosText.drawRightAligned(cs, FONT_REGULAR, SIZE_BODY,
                DATETIME_FMT.format(sale.soldAt()), rightX, y);
        y -= LINE_HEIGHT;

        if (notBlank(sale.customerName())) {
            PosText.drawAt(cs, FONT_REGULAR, SIZE_BODY, "Customer: " + sale.customerName(), A4_MARGIN, y);
            if (notBlank(sale.customerPhone())) {
                PosText.drawRightAligned(cs, FONT_REGULAR, SIZE_BODY, sale.customerPhone(), rightX, y);
            }
            y -= LINE_HEIGHT;
        }
        if (notBlank(sale.customerPan())) {
            PosText.drawAt(cs, FONT_REGULAR, SIZE_BODY, "Customer PAN: " + sale.customerPan(), A4_MARGIN, y);
            y -= LINE_HEIGHT;
        }

        PosText.drawAt(cs, FONT_REGULAR, SIZE_BODY,
                "Payment: " + sale.paymentMethod() + " (" + sale.paymentStatus() + ")", A4_MARGIN, y);
        y -= SECTION_GAP;

        return y;
    }

    /**
     * Column x-positions, right edges for the numeric ones.
     */
    private static final float COL_QTY_RIGHT_INSET = 210f;
    private static final float COL_RATE_RIGHT_INSET = 120f;

    private float drawA4ItemHeader(PDPageContentStream cs, float y) throws IOException {
        float rightX = A4_MARGIN + A4_CONTENT_WIDTH;

        y = solidRule(cs, y, 0.5f);
        PosText.drawAt(cs, FONT_BOLD, SIZE_SMALL, "ITEM", A4_MARGIN, y);
        PosText.drawRightAligned(cs, FONT_BOLD, SIZE_SMALL, "QTY", rightX - COL_QTY_RIGHT_INSET, y);
        PosText.drawRightAligned(cs, FONT_BOLD, SIZE_SMALL, "RATE", rightX - COL_RATE_RIGHT_INSET, y);
        PosText.drawRightAligned(cs, FONT_BOLD, SIZE_SMALL, "AMOUNT", rightX, y);
        y -= 6;
        return solidRule(cs, y, 0.5f);
    }

    private float drawA4Item(PDPageContentStream cs, SaleItemResponse item, float y) throws IOException {
        float rightX = A4_MARGIN + A4_CONTENT_WIDTH;
        float nameWidth = A4_CONTENT_WIDTH - COL_QTY_RIGHT_INSET - 10;

        List<String> nameLines = PosText.wrap(FONT_REGULAR, SIZE_BODY, item.productName(), nameWidth);
        if (nameLines.isEmpty()) {
            nameLines = List.of("-");
        }

        // The numbers sit on the first line of a wrapped name, so a long item
        // name pushes the block down without ever separating a row from its total.
        PosText.drawAt(cs, FONT_REGULAR, SIZE_BODY, nameLines.getFirst(), A4_MARGIN, y);
        PosText.drawRightAligned(cs, FONT_REGULAR, SIZE_BODY,
                quantity(item.quantity()) + " " + item.unitSymbol(), rightX - COL_QTY_RIGHT_INSET, y);
        PosText.drawRightAligned(cs, FONT_REGULAR, SIZE_BODY, money(item.rate()), rightX - COL_RATE_RIGHT_INSET, y);
        PosText.drawRightAligned(cs, FONT_REGULAR, SIZE_BODY, money(item.lineTotal()), rightX, y);
        y -= LINE_HEIGHT;

        for (String continuation : nameLines.subList(1, nameLines.size())) {
            PosText.drawAt(cs, FONT_REGULAR, SIZE_BODY, continuation, A4_MARGIN, y);
            y -= LINE_HEIGHT;
        }

        if (isPositive(item.discountAmount())) {
            PosText.drawAt(cs, FONT_REGULAR, SIZE_SMALL,
                    "   less discount " + money(item.discountAmount()), A4_MARGIN, y);
            y -= LINE_HEIGHT * 0.85f;
        }

        return y;
    }

    private float drawA4Totals(PDPageContentStream cs, SaleDetailResponse sale, float y) throws IOException {
        float rightX = A4_MARGIN + A4_CONTENT_WIDTH;
        float labelRight = rightX - 90f;

        y -= 4;
        y = solidRule(cs, y, 0.5f);

        y = a4TotalLine(cs, "Sub total", money(sale.subTotal()), labelRight, rightX, y, false);
        if (isPositive(sale.discountAmount())) {
            y = a4TotalLine(cs, "Discount", "- " + money(sale.discountAmount()), labelRight, rightX, y, false);
            y = a4TotalLine(cs, "Taxable amount", money(sale.taxableAmount()), labelRight, rightX, y, false);
        }
        if (sale.taxScheme() == TaxScheme.VAT) {
            y = a4TotalLine(cs, "VAT", money(sale.vatAmount()), labelRight, rightX, y, false);
        }

        y -= 2;
        y = solidRule(cs, y, 1f);
        y = a4TotalLine(cs, "TOTAL", money(sale.netTotal()), labelRight, rightX, y, true);

        if (isPositive(sale.paidAmount())) {
            y = a4TotalLine(cs, "Paid", money(sale.paidAmount()), labelRight, rightX, y, false);
        }
        if (isPositive(sale.changeAmount())) {
            y = a4TotalLine(cs, "Change", money(sale.changeAmount()), labelRight, rightX, y, false);
        }
        if (isPositive(sale.dueAmount())) {
            y = a4TotalLine(cs, "Balance due", money(sale.dueAmount()), labelRight, rightX, y, true);
        }

        return y;
    }

    private float a4TotalLine(PDPageContentStream cs, String label, String value,
                              float labelRight, float rightX, float y, boolean bold) throws IOException {
        PDType1Font font = bold ? FONT_BOLD : FONT_REGULAR;
        PosText.drawRightAligned(cs, font, SIZE_BODY, label, labelRight, y);
        PosText.drawRightAligned(cs, font, SIZE_BODY, value, rightX, y);
        return y - LINE_HEIGHT;
    }

    private void drawA4Footer(PDPageContentStream cs, SaleDetailResponse sale) throws IOException {
        float y = A4_MARGIN + 30f;
        solidRule(cs, y, 0.5f);
        y -= 12f;

        if (notBlank(sale.remark())) {
            PosText.drawAt(cs, FONT_REGULAR, SIZE_SMALL, sale.remark(), A4_MARGIN, y);
            y -= 11f;
        }
        PosText.drawCentered(cs, FONT_REGULAR, SIZE_SMALL,
                sale.paymentStatus() == PaymentStatus.PAID
                        ? "Thank you for shopping with us."
                        : "This invoice is not yet settled in full.",
                y, A4_MARGIN, A4_CONTENT_WIDTH);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Thermal receipt
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * One item, pre-wrapped, so its height is known before the page is sized.
     */
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

    /**
     * The page height, from the very same blocks and the very same header
     * formula the drawing uses. Anything computed here and not drawn there (or
     * the reverse) shows up as a blank tail or a clipped total.
     */
    private float measureReceipt(SaleDetailResponse sale, MartBranding branding,
                                 PosLayout layout, List<ItemBlock> blocks) {

        float height = layout.margin() * 2;
        height += receiptHeaderHeight(branding, sale, layout);

        for (ItemBlock block : blocks) {
            height += block.lineCount() * layout.lineHeight();
        }

        height += layout.dividerGap() * 2;
        height += receiptTotalsHeight(sale, layout);
        height += layout.sectionGap() + layout.lineHeight() * 3;

        return height;
    }

    private float receiptHeaderHeight(MartBranding branding, SaleDetailResponse sale, PosLayout layout) {
        float height = layout.lineHeight() * 2;                         // company name + title
        if (contactLine(branding) != null) height += layout.lineHeight();
        if (notBlank(branding.registrationNumber())) height += layout.lineHeight();
        height += layout.dividerGap() * 2;
        height += layout.lineHeight() * 2;                              // invoice number + timestamp
        if (notBlank(sale.customerName())) height += layout.lineHeight();
        if (notBlank(sale.customerPan())) height += layout.lineHeight();
        height += layout.sectionGap();
        return height;
    }

    private float receiptTotalsHeight(SaleDetailResponse sale, PosLayout layout) {
        int lines = 2;                                                   // sub total + TOTAL
        if (isPositive(sale.discountAmount())) lines += 2;               // discount + taxable
        if (sale.taxScheme() == TaxScheme.VAT) lines++;
        if (isPositive(sale.paidAmount())) lines++;
        if (isPositive(sale.changeAmount())) lines++;
        if (isPositive(sale.dueAmount())) lines++;
        return lines * layout.lineHeight();
    }

    private List<ItemBlock> buildItemBlocks(SaleDetailResponse sale, PosLayout layout) {
        // The amount column is reserved out of the content width, so a long item
        // name wraps rather than running under the total.
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
        PosText.drawCentered(cs, FONT_REGULAR, layout.smallSize(),
                sale.paymentStatus() == PaymentStatus.PAID
                        ? "Thank you, please come again"
                        : "Balance outstanding",
                y, layout.margin(), layout.contentWidth());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Formatting
    // ═══════════════════════════════════════════════════════════════════════

    private static String documentTitle(SaleDetailResponse sale) {
        return sale.taxScheme() == TaxScheme.VAT ? "TAX INVOICE" : "INVOICE";
    }

    private static String contactLine(MartBranding branding) {
        return join(" · ",
                blankToNull(branding.companyAddress()),
                prefixed("Tel ", branding.companyPhone()));
    }

    /**
     * Two decimal places, always — a receipt column that ragged would be unreadable.
     */
    private static String money(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Trailing zeros dropped: "2" rather than "2.000000" for a quantity of two.
     */
    private static String quantity(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
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

    /**
     * Joins only the parts that are present, so no separator ever dangles.
     */
    private static String join(String separator, String... parts) {
        List<String> present = new ArrayList<>();
        for (String part : parts) {
            if (notBlank(part)) {
                present.add(part);
            }
        }
        return present.isEmpty() ? null : String.join(separator, present);
    }

    private float solidRule(PDPageContentStream cs, float y, float thickness) throws IOException {
        cs.setLineWidth(thickness);
        cs.moveTo(A4_MARGIN, y);
        cs.lineTo(A4_MARGIN + A4_CONTENT_WIDTH, y);
        cs.stroke();
        return y - SECTION_GAP;
    }
}
