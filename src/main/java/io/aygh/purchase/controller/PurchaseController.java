package io.aygh.purchase.controller;

import io.aygh.purchase.dto.request.PurchaseRequest;
import io.aygh.purchase.dto.response.PurchaseDetailResponse;
import io.aygh.purchase.dto.response.PurchaseSummaryResponse;
import io.aygh.purchase.service.command.PurchaseCommandService;
import io.aygh.purchase.service.query.PurchaseQueryService;
import io.aygh.shared.response.ApiResponse;
import io.aygh.shared.response.PageableRequest;
import io.aygh.shared.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Goods bought from vendors.
 * <p>
 * Backs the Purchasing group of the sidebar. Recording a purchase moves stock
 * and a vendor's balance, so it is guarded more narrowly than reading one — the
 * store keeper who receives the goods is not who commits the mart to paying for
 * them.
 */
@Tag(name = "Purchases", description = "Goods received from vendors, and the stock and balances they move.")
@RestController
@RequestMapping("/purchases")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'STORE_KEEPER')")
public class PurchaseController {

    private final PurchaseCommandService purchaseCommandService;
    private final PurchaseQueryService purchaseQueryService;

    @Operation(summary = "Record a purchase over any number of products")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER')")
    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseDetailResponse>> create(
            @Valid @RequestBody PurchaseRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(purchaseCommandService.create(request)));
    }

    @Operation(summary = "List purchases — filter by vendor, bill number or date range")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PurchaseSummaryResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @ModelAttribute PageableRequest pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                purchaseQueryService.findAll(search, vendorId, from, to, pageable.toPageable())));
    }

    @Operation(summary = "One purchase, lines included")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseDetailResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(purchaseQueryService.findById(id)));
    }
}
