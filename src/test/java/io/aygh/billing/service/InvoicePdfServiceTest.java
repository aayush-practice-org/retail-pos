package io.aygh.billing.service;

import io.aygh.sales.dto.response.SaleDetailResponse;
import io.aygh.sales.dto.response.SaleItemResponse;
import io.aygh.sales.dto.response.SalesBookResponse;
import io.aygh.sales.dto.response.SalesBookRowResponse;
import io.aygh.sales.dto.response.SalesBookTotalResponse;
import io.aygh.sales.entity.SaleChannel;
import io.aygh.shared.entity.PaymentMethod;
import io.aygh.shared.entity.PaymentStatus;
import io.aygh.shared.entity.TaxScheme;
import io.aygh.shared.print.PosPaper;
import io.aygh.shared.response.PrintPaperType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoicePdfServiceTest {

    private InvoicePdfService pdfService;

    static class StubBrandingService extends MartBrandingService {
        StubBrandingService() {
            super(null);
        }

        @Override
        public MartBranding resolve() {
            return new MartBranding(
                    "Aygh Retail Mart",
                    "Kathmandu, Nepal",
                    "9800000000",
                    "info@ayghmart.com",
                    "123456789"
            );
        }
    }

    @BeforeEach
    void setUp() {
        pdfService = new InvoicePdfService(new StubBrandingService());
    }

    private SaleDetailResponse sampleSale(int printCount) {
        return new SaleDetailResponse(
                1L,
                "INV-2081-0001",
                Instant.now(),
                SaleChannel.POS,
                TaxScheme.VAT,
                10L,
                "John Doe",
                "9811111111",
                "987654321",
                "2081.09.05",
                "2081.082",
                new BigDecimal("1000.00"),
                new BigDecimal("50.00"),
                new BigDecimal("950.00"),
                new BigDecimal("123.50"),
                new BigDecimal("1073.50"),
                PaymentMethod.CASH,
                PaymentStatus.PAID,
                new BigDecimal("1100.00"),
                new BigDecimal("26.50"),
                BigDecimal.ZERO,
                "Test remark",
                printCount,
                printCount > 0,
                true,
                List.of(
                        new SaleItemResponse(1L, 101L, "Wai Wai Noodles", "WAI01", 1L, "Pcs",
                                new BigDecimal("5"), new BigDecimal("5"), new BigDecimal("30.00"),
                                new BigDecimal("30.00"), BigDecimal.ZERO, new BigDecimal("150.00")),
                        new SaleItemResponse(2L, 102L, "Amul Butter 500g", "BUT01", 2L, "Pkt",
                                new BigDecimal("2"), new BigDecimal("2"), new BigDecimal("425.00"),
                                new BigDecimal("450.00"), new BigDecimal("50.00"), new BigDecimal("850.00"))
                )
        );
    }

    @Test
    void testRenderReceiptThermal() {
        byte[] pdf = pdfService.renderReceipt(sampleSale(1), PosPaper.MM_80);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testRenderReceiptCopy() {
        byte[] pdf = pdfService.renderReceipt(sampleSale(2), PosPaper.MM_80);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testRenderTaxInvoiceAllPaperTypes() {
        for (PrintPaperType paperType : PrintPaperType.values()) {
            byte[] pdf = pdfService.renderTaxInvoice(sampleSale(1), paperType);
            assertNotNull(pdf, "PDF should not be null for paper type: " + paperType);
            assertTrue(pdf.length > 0, "PDF should contain bytes for paper type: " + paperType);

            byte[] copyPdf = pdfService.renderTaxInvoice(sampleSale(3), paperType);
            assertNotNull(copyPdf, "Copy PDF should not be null for paper type: " + paperType);
            assertTrue(copyPdf.length > 0, "Copy PDF should contain bytes for paper type: " + paperType);
        }
    }

    /** A PAN mart's bill: no VAT, the whole amount is the total. */
    private SaleDetailResponse panSale() {
        return new SaleDetailResponse(
                2L, "INV-2081-0002", Instant.now(), SaleChannel.POS, TaxScheme.NON_VAT,
                null, null, null, null, "2081.09.05", "2081.082",
                new BigDecimal("300.00"), BigDecimal.ZERO, new BigDecimal("300.00"),
                BigDecimal.ZERO, new BigDecimal("300.00"),
                PaymentMethod.CASH, PaymentStatus.PAID, new BigDecimal("300.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, null, 1, true, true,
                List.of(new SaleItemResponse(1L, 101L, "Wai Wai Noodles", "WAI01", 1L, "Pcs",
                        new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("30.00"),
                        new BigDecimal("30.00"), BigDecimal.ZERO, new BigDecimal("300.00"))));
    }

    /** A VAT mart whose shelf prices carry VAT: 113 billed is 100 taxable + 13 VAT. */
    private SaleDetailResponse vatIncludedSale() {
        return new SaleDetailResponse(
                3L, "INV-2081-0003", Instant.now(), SaleChannel.POS, TaxScheme.VAT,
                null, null, null, null, "2081.09.05", "2081.082",
                new BigDecimal("113.00"), BigDecimal.ZERO, new BigDecimal("100.00"),
                new BigDecimal("13.00"), new BigDecimal("113.00"),
                PaymentMethod.CASH, PaymentStatus.PAID, new BigDecimal("113.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, null, 1, true, true,
                List.of(new SaleItemResponse(1L, 101L, "Amul Butter 500g", "BUT01", 1L, "Pkt",
                        new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("113.00"),
                        new BigDecimal("113.00"), BigDecimal.ZERO, new BigDecimal("113.00"))));
    }

    private static String text(byte[] pdf) throws IOException {
        try (var doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void panBillIsAPlainInvoiceStampedAsNotAVatBill() throws IOException {
        for (PrintPaperType paperType : PrintPaperType.values()) {
            String text = text(pdfService.renderTaxInvoice(panSale(), paperType));
            assertTrue(text.contains("THIS IS NOT A VAT OR PAN BILL"), "notice missing on " + paperType);
            assertFalse(text.toUpperCase().contains("TAX INVOICE"), "PAN bill titled as tax invoice on " + paperType);
            assertFalse(text.contains("VAT 13 %"), "VAT row on PAN bill on " + paperType);
        }
    }

    @Test
    void vatBillIsATaxInvoiceWithoutThePanNotice() throws IOException {
        for (PrintPaperType paperType : PrintPaperType.values()) {
            String text = text(pdfService.renderTaxInvoice(sampleSale(1), paperType));
            assertTrue(text.toUpperCase().contains("TAX INVOICE"), "title missing on " + paperType);
            assertTrue(text.contains("VAT 13 %"), "VAT row missing on " + paperType);
            assertFalse(text.contains("THIS IS NOT A VAT OR PAN BILL"), "PAN notice on VAT bill on " + paperType);
        }
    }

    @Test
    void vatInclusiveLinesArePrintedExclusiveOfVat() throws IOException {
        for (PrintPaperType paperType : PrintPaperType.values()) {
            String text = text(pdfService.renderVatInvoice(vatIncludedSale(), paperType));
            // Rate, line total and taxable row all read 100.00; left VAT-inclusive,
            // only the taxable row would, and the form would not add up.
            long taxableFigures = text.lines().flatMap(l -> java.util.Arrays.stream(l.split("\\s+")))
                    .filter("100.00"::equals).count();
            assertTrue(taxableFigures >= 3, "lines not printed exclusive of VAT on " + paperType + ":\n" + text);
        }
    }

    @Test
    void testGenerateSalesBook() {
        SalesBookResponse book = new SalesBookResponse(
                "Aygh Retail Mart",
                "123456789",
                "2026-09-01 to 2026-09-30",
                List.of(
                        new SalesBookRowResponse("2081.09.05", "INV-2081-0001", "John Doe", "987654321",
                                new BigDecimal("1073.50"), BigDecimal.ZERO, BigDecimal.ZERO,
                                new BigDecimal("50.00"), new BigDecimal("950.00"), new BigDecimal("123.50"))
                ),
                new SalesBookTotalResponse(
                        new BigDecimal("1073.50"), BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("50.00"), new BigDecimal("950.00"), new BigDecimal("123.50")
                )
        );

        byte[] pdf = pdfService.generateSalesBook(book);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }
}
