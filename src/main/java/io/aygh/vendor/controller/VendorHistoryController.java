package io.aygh.vendor.controller;

import io.aygh.shared.response.ApiResponse;
import io.aygh.shared.response.PageableRequest;
import io.aygh.shared.response.PagedResponse;
import io.aygh.vendor.dto.response.VendorHistoryResponse;
import io.aygh.vendor.service.query.VendorHistoryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * What has been bought, and from whom.
 * <p>
 * Read-only: rows appear here as a consequence of recording a purchase, through
 * {@code VendorHistoryCommandService}, never by being typed in.
 */
@Tag(name = "Vendors · History", description = "The purchase trail, by vendor.")
@RestController
@RequestMapping("/vendors")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'ACCOUNTANT')")
public class VendorHistoryController {

    private final VendorHistoryQueryService vendorHistoryQueryService;

    @Operation(summary = "Every vendor's purchase trail")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PagedResponse<VendorHistoryResponse>>> list(
            @ModelAttribute PageableRequest pageable) {

        return ResponseEntity.ok(ApiResponse.ok(vendorHistoryQueryService.findAll(pageable.toPageable())));
    }

    @Operation(summary = "One history entry")
    @GetMapping("/history/{id}")
    public ResponseEntity<ApiResponse<VendorHistoryResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(vendorHistoryQueryService.findById(id)));
    }

    @Operation(summary = "One vendor's purchase trail")
    @GetMapping("/{vendorId}/history")
    public ResponseEntity<ApiResponse<PagedResponse<VendorHistoryResponse>>> byVendor(
            @PathVariable Long vendorId, @ModelAttribute PageableRequest pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                vendorHistoryQueryService.findByVendorId(vendorId, pageable.toPageable())));
    }
}
