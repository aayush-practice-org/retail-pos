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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
