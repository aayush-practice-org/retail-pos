package io.aygh.sales.controller;

import io.aygh.sales.dto.request.SalePaymentRequest;
import io.aygh.sales.dto.request.SaleRequest;
import io.aygh.sales.dto.response.SaleDetailResponse;
import io.aygh.sales.dto.response.SalesReportSummary;
import io.aygh.sales.dto.response.SaleSummaryResponse;
import io.aygh.sales.dto.response.SalesTotalsResponse;
import io.aygh.sales.service.command.SaleCommandService;
import io.aygh.sales.service.query.SaleQueryService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Bills raised, and the money taken against them.
 * <p>
 * Backs both the Sales group of the sidebar and the till: a POS sale and a
 * back-office one are the same document, distinguished by
 * {@code SaleRequest#channel}, so there is one endpoint rather than two that
 * would have to be kept in step.
 */
@Tag(name = "Sales", description = "Bills, the stock they move, and the payments taken against them.")
@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'SALES_EXECUTIVE', 'ACCOUNTANT', 'CASHIER')")
public class SaleController {

    private final SaleCommandService saleCommandService;
    private final SaleQueryService saleQueryService;

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

    @Operation(summary = "Take payment against an unpaid or part-paid bill")
    @PostMapping("/{id}/payments")
    public ResponseEntity<ApiResponse<SaleDetailResponse>> pay(
            @PathVariable Long id, @Valid @RequestBody SalePaymentRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Payment recorded", saleCommandService.pay(id, request)));
    }
}
