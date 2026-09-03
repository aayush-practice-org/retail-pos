package io.aygh.vendor.controller;

import io.aygh.shared.response.ApiResponse;
import io.aygh.shared.response.PageableRequest;
import io.aygh.shared.response.PagedResponse;
import io.aygh.vendor.dto.request.VendorRequest;
import io.aygh.vendor.dto.response.VendorResponse;
import io.aygh.vendor.service.command.VendorCommandService;
import io.aygh.vendor.service.query.VendorQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The suppliers a mart buys from.
 * <p>
 * The tenant is never a parameter: these rows live in the caller's own schema,
 * chosen from its token, so there is nothing on the wire to tamper with.
 * <p>
 * The guard is the role set behind {@code SidebarMenu.VENDOR} — a menu entry
 * that leads to a 403 is worse than no entry at all.
 */
@Tag(name = "Vendors", description = "Suppliers, and their master data.")
@RestController
@RequestMapping("/vendors")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER', 'ACCOUNTANT')")
public class VendorController {

    private final VendorCommandService vendorCommandService;
    private final VendorQueryService vendorQueryService;

    @Operation(summary = "Register a vendor")
    @PostMapping
    public ResponseEntity<ApiResponse<VendorResponse>> create(@Valid @RequestBody VendorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(vendorCommandService.create(request)));
    }

    @Operation(summary = "List vendors — matches on name, contact number or PAN")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<VendorResponse>>> list(
            @RequestParam(required = false) String search,
            @ModelAttribute PageableRequest pageable) {

        return ResponseEntity.ok(ApiResponse.ok(vendorQueryService.findAll(search, pageable.toPageable())));
    }

    @Operation(summary = "Every vendor, unpaged — for the pickers that choose one")
    @GetMapping("/selection")
    public ResponseEntity<ApiResponse<List<VendorResponse>>> selection() {
        return ResponseEntity.ok(ApiResponse.ok(vendorQueryService.findAllForSelection()));
    }

    @Operation(summary = "One vendor")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(vendorQueryService.findById(id)));
    }

    @Operation(summary = "Edit a vendor")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorResponse>> update(
            @PathVariable Long id, @Valid @RequestBody VendorRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Vendor updated", vendorCommandService.update(id, request)));
    }

    @Operation(summary = "Remove a vendor nothing is recorded against")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        vendorCommandService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Vendor removed"));
    }
}
