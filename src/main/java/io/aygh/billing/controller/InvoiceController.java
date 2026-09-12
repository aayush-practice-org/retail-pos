package io.aygh.billing.controller;

import io.aygh.billing.service.InvoicePdfService;
import io.aygh.sales.dto.response.SaleDetailResponse;
import io.aygh.sales.service.query.SaleQueryService;
import io.aygh.shared.response.PrintPaperType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * The single printable tax invoice endpoint.
 * <p>
 * Handles all paper formats (thermal rolls MM80, MM75 and standard page sizes A4, A5, A6).
 * Automatically increments and tracks print count ("COPY OF ORIGINAL (N)" for reprints).
 */
@Tag(name = "Sales · Invoices", description = "Printable IRD tax invoices.")
@RestController
@RequestMapping("/sales/{saleId}")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'SALES_EXECUTIVE', 'ACCOUNTANT', 'CASHIER')")
public class InvoiceController {

    private final InvoicePdfService invoicePdfService;
    private final SaleQueryService saleQueryService;

    @Operation(summary = "The IRD tax invoice form for any paper type (MM80, MM75, A4, A5, A6)")
    @GetMapping(value = "/tax-invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateTaxInvoicePdf(
            @PathVariable Long saleId,
            @RequestParam(required = false, defaultValue = "MM80") PrintPaperType paperType) {

        SaleDetailResponse sale = saleQueryService.incrementPrintCount(saleId);
        byte[] pdfBytes = invoicePdfService.renderTaxInvoice(sale, paperType != null ? paperType : PrintPaperType.MM80);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tax-invoice-" + sale.invoiceNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
