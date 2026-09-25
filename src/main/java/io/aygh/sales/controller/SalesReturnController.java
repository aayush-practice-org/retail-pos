package io.aygh.sales.controller;

import io.aygh.sales.dto.request.SalesReturnRequest;
import io.aygh.sales.dto.response.SalesReturnResponse;
import io.aygh.sales.service.command.SalesReturnCommandService;
import io.aygh.sales.service.query.SalesReturnQueryService;
import io.aygh.shared.response.ApiResponse;
import io.aygh.shared.response.DateRange;
import io.aygh.shared.response.PageableRequest;
import io.aygh.shared.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Goods handed back against a bill, each raised as a credit note.
 * <p>
 * A return puts the stock back on the shelf and credits the bill: on a credit
 * bill it comes off what the customer owes, and anything they had already paid
 * beyond the reduced bill is refunded.
 */
@Tag(name = "Sales · Returns", description = "Credit notes: goods handed back against a bill.")
@RestController
@RequestMapping("/sales-returns")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'SALES_EXECUTIVE', 'ACCOUNTANT', 'CASHIER')")
public class SalesReturnController {

    private final SalesReturnCommandService commandService;
    private final SalesReturnQueryService queryService;

    @Operation(summary = "Take goods back against a bill — raises the credit note and restocks the shelf")
    @PostMapping
    public ResponseEntity<ApiResponse<SalesReturnResponse>> create(@Valid @RequestBody SalesReturnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(commandService.create(request)));
    }

    @Operation(summary = "List credit notes — filter by credit note, invoice number, customer or date range")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SalesReturnResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DateRange dateRange,
            @ModelAttribute PageableRequest pageable) {

        return ResponseEntity.ok(ApiResponse.ok(queryService.findAll(search, dateRange, pageable.toPageable())));
    }

    @Operation(summary = "One credit note, lines included")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SalesReturnResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.findById(id)));
    }

    @Operation(summary = "Every credit note raised against one bill")
    @GetMapping("/by-sale/{saleId}")
    public ResponseEntity<ApiResponse<List<SalesReturnResponse>>> bySale(@PathVariable Long saleId) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.findBySale(saleId)));
    }
}
