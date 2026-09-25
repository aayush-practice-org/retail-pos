package io.aygh.purchase.controller;

import io.aygh.purchase.dto.request.PurchaseReturnRequest;
import io.aygh.purchase.dto.response.PurchaseReturnResponse;
import io.aygh.purchase.service.command.PurchaseReturnCommandService;
import io.aygh.purchase.service.query.PurchaseReturnQueryService;
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
 * Goods sent back to vendors — damaged, expired or wrong — line by line against
 * the bill they came on, each raised as a debit note. Guarded like recording a
 * purchase: sending goods back moves stock and a vendor's balance.
 */
@Tag(name = "Purchases · Returns", description = "Debit notes: goods sent back to a vendor against one of their bills.")
@RestController
@RequestMapping("/purchase-returns")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'STORE_KEEPER', 'ACCOUNTANT')")
public class PurchaseReturnController {

    private final PurchaseReturnCommandService commandService;
    private final PurchaseReturnQueryService queryService;

    @Operation(summary = "Send goods back against a vendor bill — raises the debit note and takes the stock off the shelf")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER')")
    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseReturnResponse>> create(@Valid @RequestBody PurchaseReturnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(commandService.create(request)));
    }

    @Operation(summary = "List debit notes — filter by vendor, debit note, bill number or date range")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PurchaseReturnResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) DateRange dateRange,
            @ModelAttribute PageableRequest pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                queryService.findAll(search, vendorId, dateRange, pageable.toPageable())));
    }

    @Operation(summary = "One debit note, lines included")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseReturnResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.findById(id)));
    }

    @Operation(summary = "Every debit note raised against one vendor bill")
    @GetMapping("/by-purchase/{purchaseId}")
    public ResponseEntity<ApiResponse<List<PurchaseReturnResponse>>> byPurchase(@PathVariable Long purchaseId) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.findByPurchase(purchaseId)));
    }
}
