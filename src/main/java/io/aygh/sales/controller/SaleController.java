package io.aygh.sales.controller;

import io.aygh.sales.dto.request.SaleRequest;
import io.aygh.sales.dto.response.SaleDetailResponse;
import io.aygh.sales.dto.response.SalesReportSummary;
import io.aygh.sales.dto.response.SaleSummaryResponse;
import io.aygh.sales.dto.response.SalesBookResponse;
import io.aygh.sales.dto.response.SalesTotalsResponse;
import io.aygh.sales.service.command.SaleCommandService;
import io.aygh.sales.service.query.SaleQueryService;
import io.aygh.sales.service.query.SalesBookQueryService;
import io.aygh.shared.entity.PaymentStatus;
import io.aygh.shared.response.ApiResponse;
import io.aygh.shared.response.DateRange;
import io.aygh.shared.response.PageableRequest;
import io.aygh.shared.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Bills raised and the stock they move.
 * <p>
 * Backs both the Sales group of the sidebar and the till: a POS sale and a
 * back-office one are the same document, distinguished by
 * {@code SaleRequest#channel}, so there is one endpoint rather than two that
 * would have to be kept in step.
 * <p>
 * Payments against credit bills are recorded through the customer settlement
 * endpoint: {@code POST /customers/{id}/settle}.
 */
@Tag(name = "Sales", description = "Bills and the stock they move. Credit payments go through POST /customers/{id}/settle.")
@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'SALES_EXECUTIVE', 'ACCOUNTANT', 'CASHIER')")
public class SaleController {

    private final SaleCommandService saleCommandService;
    private final SaleQueryService saleQueryService;
    private final SalesBookQueryService salesBookQueryService;

    @Operation(summary = "Ring up a basket — raises the bill and takes the stock off the shelf")
    @PostMapping
    public ResponseEntity<ApiResponse<SaleDetailResponse>> create(@Valid @RequestBody SaleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(saleCommandService.create(request)));
    }

    @Operation(summary = "List bills — filter by invoice, customer, payment status or date range")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SaleSummaryResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) DateRange dateRange,
            @ModelAttribute PageableRequest pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                saleQueryService.findAll(search, status, dateRange, pageable.toPageable())));
    }

    @Operation(summary = "What was sold over a window")
    @GetMapping("/totals")
    public ResponseEntity<ApiResponse<SalesTotalsResponse>> totals(
            @RequestParam(required = false) DateRange dateRange) {

        return ResponseEntity.ok(ApiResponse.ok(saleQueryService.totals(dateRange)));
    }

    @Operation(summary = "Summary report for sales over a date range")
    @GetMapping("/report")
    public ResponseEntity<ApiResponse<SalesReportSummary>> report(
            @RequestParam(defaultValue = "THIS_MONTH") DateRange dateRange) {

        return ResponseEntity.ok(ApiResponse.ok(saleQueryService.salesReport(dateRange)));
    }

    @Operation(summary = "The IRD sales book for a period: a line per bill and the column totals")
    @GetMapping("/sales-book")
    public ResponseEntity<ApiResponse<SalesBookResponse>> getSalesBook(
            @RequestParam(required = false, defaultValue = "THIS_MONTH") DateRange dateRange) {

        return ResponseEntity.ok(ApiResponse.ok("Sales book fetched successfully",
                salesBookQueryService.getSalesBook(dateRange)));
    }

    @Operation(summary = "The same IRD sales book rendered on the IRD form, as a landscape A4 PDF")
    @GetMapping(value = "/sales-book/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateSalesBookPdf(
            @RequestParam(required = false, defaultValue = "THIS_MONTH") DateRange dateRange) {

        byte[] pdfBytes = salesBookQueryService.generateSalesBookPdf(dateRange);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sales-book.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @Operation(summary = "One bill, lines included")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleDetailResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(saleQueryService.findById(id)));
    }

    @Operation(summary = "One bill by the number printed on it")
    @GetMapping("/by-invoice/{invoiceNumber}")
    public ResponseEntity<ApiResponse<SaleDetailResponse>> getByInvoiceNumber(
            @PathVariable String invoiceNumber) {

        return ResponseEntity.ok(ApiResponse.ok(saleQueryService.findByInvoiceNumber(invoiceNumber)));
    }
}
