package io.aygh.inventory.controller;

import io.aygh.inventory.dto.request.ProductCreateRequest;
import io.aygh.inventory.dto.request.ProductPurchaseUnitCreateRequest;
import io.aygh.inventory.dto.request.ProductPurchaseUnitUpdateRequest;
import io.aygh.inventory.dto.request.ProductSellingUnitCreateRequest;
import io.aygh.inventory.dto.request.ProductSellingUnitUpdateRequest;
import io.aygh.inventory.dto.request.ProductUpdateRequest;
import io.aygh.inventory.dto.request.ProductVatRequest;
import io.aygh.inventory.dto.response.ProductDetailResponse;
import io.aygh.inventory.dto.response.ProductPurchaseUnitDetailResponse;
import io.aygh.inventory.dto.response.ProductPurchaseUnitResponse;
import io.aygh.inventory.dto.response.ProductSellingUnitResponse;
import io.aygh.inventory.dto.response.ProductSummaryResponse;
import io.aygh.inventory.service.command.ProductCommandService;
import io.aygh.inventory.service.query.ProductQueryService;
import io.aygh.shared.response.ApiResponse;
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

import java.util.List;

/**
 * The catalogue, and how each product is bought and sold.
 * <p>
 * Three response shapes, each on its own endpoint, so nothing returns more than
 * the screen behind it needs: a listed row is a {@code ProductSummaryResponse};
 * the single-product view is a {@code ProductDetailResponse}, which extends it
 * with the long text and the trading configuration; and a purchase unit's VAT
 * history is a further endpoint again, because an audit trail has no business
 * in a product payload.
 * <p>
 * The trading configuration is nested under its product rather than exposed as a
 * resource of its own: a purchase unit means nothing apart from the product it
 * configures, and nesting it means the product is never a body field a caller
 * could point somewhere else.
 */
@Tag(name = "Inventory · Products", description = "The catalogue, and how each product is traded.")
@RestController
@RequestMapping("/inventory/products")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
public class ProductController {

    private final ProductCommandService productCommandService;
    private final ProductQueryService productQueryService;

    // ── Catalogue ─────────────────────────────────────────────────────────

    @Operation(summary = "Add a product")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductSummaryResponse>> create(
            @Valid @RequestBody ProductCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(productCommandService.create(request)));
    }

    @Operation(summary = "List products — summaries only, no trading configuration")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductSummaryResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @ModelAttribute PageableRequest pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                productQueryService.findAll(search, categoryId, active, pageable.toPageable())));
    }

    @Operation(summary = "One product in full — base unit, purchase units, selling units")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productQueryService.findById(id)));
    }

    @Operation(summary = "What the till resolves a scan to")
    @GetMapping("/by-barcode/{barcode}")
    public ResponseEntity<ApiResponse<ProductSellingUnitResponse>> byBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(ApiResponse.ok(productQueryService.findByBarcode(barcode)));
    }

    @Operation(summary = "Edit a product — category and base unit are fixed at creation")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductSummaryResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Product updated", productCommandService.update(id, request)));
    }

    @Operation(summary = "Retire a product")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productCommandService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Product retired"));
    }

    // ── How the product is bought ─────────────────────────────────────────

    @Operation(summary = "Configure how this product is bought")
    @PostMapping("/{id}/purchase-units")
    public ResponseEntity<ApiResponse<ProductPurchaseUnitDetailResponse>> addPurchaseUnit(
            @PathVariable Long id, @Valid @RequestBody ProductPurchaseUnitCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(productCommandService.addPurchaseUnit(id, request)));
    }

    @Operation(summary = "The units this product is bought in")
    @GetMapping("/{id}/purchase-units")
    public ResponseEntity<ApiResponse<List<ProductPurchaseUnitResponse>>> purchaseUnits(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productQueryService.findPurchaseUnits(id)));
    }

    @Operation(summary = "One purchase unit, with its full VAT history")
    @GetMapping("/{id}/purchase-units/{purchaseUnitId}")
    public ResponseEntity<ApiResponse<ProductPurchaseUnitDetailResponse>> purchaseUnit(
            @PathVariable Long id, @PathVariable Long purchaseUnitId) {

        return ResponseEntity.ok(ApiResponse.ok(productQueryService.findPurchaseUnit(id, purchaseUnitId)));
    }

    @Operation(summary = "Edit a purchase unit — the unit itself is fixed")
    @PutMapping("/{id}/purchase-units/{purchaseUnitId}")
    public ResponseEntity<ApiResponse<ProductPurchaseUnitDetailResponse>> updatePurchaseUnit(
            @PathVariable Long id, @PathVariable Long purchaseUnitId,
            @Valid @RequestBody ProductPurchaseUnitUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Purchase unit updated",
                productCommandService.updatePurchaseUnit(id, purchaseUnitId, request)));
    }

    @Operation(summary = "Remove a purchase unit")
    @DeleteMapping("/{id}/purchase-units/{purchaseUnitId}")
    public ResponseEntity<ApiResponse<Void>> removePurchaseUnit(
            @PathVariable Long id, @PathVariable Long purchaseUnitId) {

        productCommandService.removePurchaseUnit(id, purchaseUnitId);
        return ResponseEntity.ok(ApiResponse.ok("Purchase unit removed"));
    }

    @Operation(summary = "Open a new VAT rate, closing the one currently in force")
    @PostMapping("/{id}/purchase-units/{purchaseUnitId}/vat")
    public ResponseEntity<ApiResponse<ProductPurchaseUnitDetailResponse>> addVatRate(
            @PathVariable Long id, @PathVariable Long purchaseUnitId,
            @Valid @RequestBody ProductVatRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(productCommandService.addVatRate(id, purchaseUnitId, request)));
    }

    // ── How the product is sold ───────────────────────────────────────────

    @Operation(summary = "Configure how this product is sold")
    @PostMapping("/{id}/selling-units")
    public ResponseEntity<ApiResponse<ProductSellingUnitResponse>> addSellingUnit(
            @PathVariable Long id, @Valid @RequestBody ProductSellingUnitCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(productCommandService.addSellingUnit(id, request)));
    }

    @Operation(summary = "The units this product is sold in")
    @GetMapping("/{id}/selling-units")
    public ResponseEntity<ApiResponse<List<ProductSellingUnitResponse>>> sellingUnits(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productQueryService.findSellingUnits(id)));
    }

    @Operation(summary = "Edit a selling unit — the unit itself is fixed")
    @PutMapping("/{id}/selling-units/{sellingUnitId}")
    public ResponseEntity<ApiResponse<ProductSellingUnitResponse>> updateSellingUnit(
            @PathVariable Long id, @PathVariable Long sellingUnitId,
            @Valid @RequestBody ProductSellingUnitUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Selling unit updated",
                productCommandService.updateSellingUnit(id, sellingUnitId, request)));
    }

    @Operation(summary = "Remove a selling unit")
    @DeleteMapping("/{id}/selling-units/{sellingUnitId}")
    public ResponseEntity<ApiResponse<Void>> removeSellingUnit(
            @PathVariable Long id, @PathVariable Long sellingUnitId) {

        productCommandService.removeSellingUnit(id, sellingUnitId);
        return ResponseEntity.ok(ApiResponse.ok("Selling unit removed"));
    }
}
