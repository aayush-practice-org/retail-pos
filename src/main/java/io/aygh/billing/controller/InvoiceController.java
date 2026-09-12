package io.aygh.billing.controller;

import io.aygh.billing.service.InvoicePdfService;
import io.aygh.sales.dto.response.SaleDetailResponse;
import io.aygh.sales.service.query.SaleQueryService;
import io.aygh.shared.print.PosPaper;
import io.aygh.shared.response.PrintPaperType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * The printable copies of a bill.
 * <p>
 * Supports both thermal rolls (MM80, MM75) and standard responsive page layouts (A4, A5, A6).
 */
@Tag(name = "Sales · Invoices", description = "Printable invoices and till receipts.")
@RestController
@RequestMapping("/sales/{saleId}/invoice")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'SALES_EXECUTIVE', 'ACCOUNTANT', 'CASHIER')")
public class InvoiceController {

    private final InvoicePdfService invoicePdfService;
    private final SaleQueryService saleQueryService;

    @Operation(summary = "The invoice PDF with configurable paper type (A4, A5, A6, MM80, MM75)")
    @GetMapping(produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getInvoicePdf(
            @PathVariable Long saleId,
            @RequestParam(required = false, defaultValue = "A4") PrintPaperType paperType) {

        SaleDetailResponse sale = saleQueryService.findById(saleId);
        byte[] body = invoicePdfService.renderTaxInvoice(sale, paperType != null ? paperType : PrintPaperType.A4);
        return pdf(body, sale.invoiceNumber() + ".pdf");
    }

    @Operation(summary = "The IRD tax invoice form (matching restaurant-kiosk shape)")
    @GetMapping(value = "/tax-invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getTaxInvoicePdf(
            @PathVariable Long saleId,
            @RequestParam(required = false, defaultValue = "MM80") PrintPaperType paperType) {

        SaleDetailResponse sale = saleQueryService.findById(saleId);
        byte[] body = invoicePdfService.renderTaxInvoice(sale, paperType != null ? paperType : PrintPaperType.MM80);
        return pdf(body, "tax-invoice-" + sale.invoiceNumber() + ".pdf");
    }

    @Operation(summary = "The till receipt, on any supported roll width")
    @GetMapping(value = "/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> receipt(
            @PathVariable Long saleId,
            @RequestParam(required = false) Float paperWidthMm) {

        SaleDetailResponse sale = saleQueryService.findById(saleId);
        byte[] body = invoicePdfService.renderReceipt(sale, PosPaper.orDefault(paperWidthMm));
        return pdf(body, sale.invoiceNumber() + "-receipt.pdf");
    }

    private ResponseEntity<byte[]> pdf(byte[] body, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(filename).build().toString())
                .body(body);
    }
}
