package io.aygh.catalog.controller;

import io.aygh.catalog.dto.request.ProductRequest;
import io.aygh.catalog.dto.request.ProductVariantRequest;
import io.aygh.catalog.dto.response.ProductDetailResponse;
import io.aygh.catalog.dto.response.ProductResponse;
import io.aygh.catalog.dto.response.ProductVariantResponse;
import io.aygh.catalog.service.command.ProductCommandService;
import io.aygh.catalog.service.command.ProductVariantCommandService;
import io.aygh.catalog.service.query.ProductQueryService;
import io.aygh.catalog.service.query.ProductVariantQueryService;
import io.aygh.shared.response.ApiResponse;
import io.aygh.shared.response.PageableRequest;
import io.aygh.shared.response.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 *  Admin Product controller
 *
 *  Products sit in a category and are tracked in one of the system-defined base
 *  units. Their variants hang off them as a sub-resource: a variant is created,
 *  edited and deleted under its own product and belongs to nothing else.
 */
@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@Slf4j
public class AdminProductController {

    private final ProductCommandService productCommandService;
    private final ProductQueryService productQueryService;
    private final ProductVariantCommandService variantCommandService;
    private final ProductVariantQueryService variantQueryService;

    // ── CRUD ──────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        log.info("ADMIN REST request to create Product: {}", request.name());
        ProductResponse response = productCommandService.create(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Product '%s' created successfully".formatted(request.name()), response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        log.info("ADMIN REST request to update Product: {}, {}", id, request.name());
        ProductResponse response = productCommandService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Product '%s' updated successfully".formatted(request.name()), response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("ADMIN REST request to delete Product: {}", id);
        productCommandService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(
                "Product with id %d deleted successfully".formatted(id), null));
    }

    /**
     * @param includeSubCategories when filtering by category, also return products
     *                             filed under its subcategories (default true)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "true") boolean includeSubCategories,
            @ModelAttribute PageableRequest pageableRequest) {
        log.info("ADMIN REST request to get all Products with search: {}, categoryId: {}", search, categoryId);
        PagedResponse<ProductResponse> products = productQueryService.findAll(
                search, categoryId, isActive, includeSubCategories, pageableRequest.toPageable());
        return ResponseEntity.ok(ApiResponse.ok("Products fetched successfully", products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> findById(@PathVariable Long id) {
        log.info("ADMIN REST request to get Product by id: {}", id);
        ProductDetailResponse product = productQueryService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok("Product fetched successfully", product));
    }

    // ── Variants ──────────────────────────────────────────────────────────

    @PostMapping("/{productId}/variants")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request) {
        log.info("ADMIN REST request to create Variant on Product: {}, {}", productId, request.name());
        ProductVariantResponse response = variantCommandService.create(productId, request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Variant '%s' added to product %d successfully".formatted(request.name(), productId), response));
    }

    @GetMapping("/{productId}/variants")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getVariants(@PathVariable Long productId) {
        log.info("ADMIN REST request to get Variants for Product: {}", productId);
        List<ProductVariantResponse> response = variantQueryService.findByProduct(productId);
        return ResponseEntity.ok(ApiResponse.ok("Variants fetched successfully", response));
    }

    @GetMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> getVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        log.info("ADMIN REST request to get Variant {} for Product: {}", variantId, productId);
        ProductVariantResponse response = variantQueryService.findById(productId, variantId);
        return ResponseEntity.ok(ApiResponse.ok("Variant fetched successfully", response));
    }

    @PutMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request) {
        log.info("ADMIN REST request to update Variant {} on Product: {}", variantId, productId);
        ProductVariantResponse response = variantCommandService.update(productId, variantId, request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Variant %d updated successfully".formatted(variantId), response));
    }

    @PutMapping("/{productId}/variants/{variantId}/default")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> setDefaultVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        log.info("ADMIN REST request to set Variant {} as default on Product: {}", variantId, productId);
        ProductVariantResponse response = variantCommandService.setDefault(productId, variantId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Variant %d set as the default for product %d".formatted(variantId, productId), response));
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        log.info("ADMIN REST request to delete Variant {} from Product: {}", variantId, productId);
        variantCommandService.delete(productId, variantId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Variant %d removed from product %d successfully".formatted(variantId, productId), null));
    }
}
