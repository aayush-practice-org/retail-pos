package io.aygh.billing.controller;

import io.aygh.billing.service.InvoicePdfService;
import io.aygh.sales.dto.response.SaleDetailResponse;
import io.aygh.sales.service.query.SaleQueryService;
import io.aygh.shared.print.PosPaper;
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
 * Two renderings of one document: A4 for the copy that gets filed, and a thermal
 * roll for the one handed over. Both are built from the same sale, so they
 * cannot disagree, and both carry the mart's own details — read from the tenant
 * behind the caller's token, never from anything on the request.
 * <p>
 * These return {@code application/pdf} rather than the usual {@code ApiResponse}
 * envelope: the browser has to be able to open the URL and get a document.
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

    @Operation(summary = "The A4 invoice")
    @GetMapping(produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> a4(@PathVariable Long saleId) {
        SaleDetailResponse sale = saleQueryService.findById(saleId);
        return pdf(invoicePdfService.renderA4(sale), sale.invoiceNumber() + ".pdf");
    }

    /**
     * {@code paperWidthMm} accepts any roll in the supported range, not a fixed
     * set: the "80mm" class alone ships as 75, 76, 78 and 80, and a mart should
     * be able to name the one its printer actually feeds.
     */
    @Operation(summary = "The till receipt, on any supported roll width")
    @GetMapping(value = "/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> receipt(
            @PathVariable Long saleId,
            @RequestParam(required = false) Float paperWidthMm) {

        SaleDetailResponse sale = saleQueryService.findById(saleId);
        byte[] body = invoicePdfService.renderReceipt(sale, PosPaper.orDefault(paperWidthMm));
        return pdf(body, sale.invoiceNumber() + "-receipt.pdf");
    }

    /** Inline, so a click opens the document rather than downloading it. */
    private ResponseEntity<byte[]> pdf(byte[] body, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(filename).build().toString())
                .body(body);
    }
}
