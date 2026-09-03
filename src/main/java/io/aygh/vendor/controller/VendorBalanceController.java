package io.aygh.vendor.controller;

import io.aygh.shared.response.ApiResponse;
import io.aygh.shared.response.PageableRequest;
import io.aygh.shared.response.PagedResponse;
import io.aygh.vendor.dto.request.VendorLedgerEntryRequest;
import io.aygh.vendor.dto.request.VendorSettlementRequest;
import io.aygh.vendor.dto.response.VendorBalanceResponse;
import io.aygh.vendor.dto.response.VendorBalanceSummaryResponse;
import io.aygh.vendor.service.command.VendorBalanceCommandService;
import io.aygh.vendor.service.query.VendorBalanceQueryService;
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
 * What a vendor is owed and what it has been paid.
 * <p>
 * Reads carry the same guard as the rest of the module, because a buyer raising
 * an order needs to see where the account stands. The two writes are narrower:
 * they move money, and posting a payable or a settlement is the accountant's
 * job, not the buyer's.
 */
@Tag(name = "Vendors · Balance", description = "A vendor's ledger, its running totals, and settlements against it.")
@RestController
@RequestMapping("/vendors/{vendorId}")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'ACCOUNTANT')")
public class VendorBalanceController {

    private final VendorBalanceCommandService vendorBalanceCommandService;
    private final VendorBalanceQueryService vendorBalanceQueryService;

    @Operation(summary = "The vendor's ledger entries")
    @GetMapping("/ledger")
    public ResponseEntity<ApiResponse<PagedResponse<VendorBalanceResponse>>> ledger(
            @PathVariable Long vendorId, @ModelAttribute PageableRequest pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                vendorBalanceQueryService.findTransactions(vendorId, pageable.toPageable())));
    }

    @Operation(summary = "Where the vendor's account stands")
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<VendorBalanceSummaryResponse>> balance(@PathVariable Long vendorId) {
        return ResponseEntity.ok(ApiResponse.ok(vendorBalanceQueryService.findSummary(vendorId)));
    }

    @Operation(summary = "Post a payable or a receivable against the vendor")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    @PostMapping("/ledger")
    public ResponseEntity<ApiResponse<VendorBalanceResponse>> post(
            @PathVariable Long vendorId, @Valid @RequestBody VendorLedgerEntryRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(vendorBalanceCommandService.post(vendorId, request)));
    }

    @Operation(summary = "Record a payment made to the vendor")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    @PostMapping("/settlements")
    public ResponseEntity<ApiResponse<VendorBalanceResponse>> settle(
            @PathVariable Long vendorId, @Valid @RequestBody VendorSettlementRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(vendorBalanceCommandService.settle(vendorId, request)));
    }
}
