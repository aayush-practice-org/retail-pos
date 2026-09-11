package io.aygh.inventory.controller;

import io.aygh.inventory.dto.request.CategoryRequest;
import io.aygh.inventory.dto.request.CategoryUnitRequest;
import io.aygh.inventory.dto.response.CategoryDetailResponse;
import io.aygh.inventory.dto.response.CategorySummaryResponse;
import io.aygh.inventory.dto.response.UnitResponse;
import io.aygh.inventory.entity.UnitUsage;
import io.aygh.inventory.service.command.CategoryCommandService;
import io.aygh.inventory.service.query.CategoryQueryService;
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
 * Categories, and the unit policy the products inside them inherit.
 * <p>
 * The list and the single-category endpoint deliberately return different
 * shapes: {@code CategorySummaryResponse} for a page of rows, and
 * {@code CategoryDetailResponse} — which extends it — for the one that also
 * carries the permitted units. A caller listing categories is never handed a
 * unit policy it did not ask for.
 */
@Tag(name = "Inventory · Categories", description = "Product groupings and the units they permit.")
@RestController
@RequestMapping("/inventory/categories")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
public class CategoryController {

    private final CategoryCommandService categoryCommandService;
    private final CategoryQueryService categoryQueryService;

    @Operation(summary = "Create a category")
    @PostMapping
    public ResponseEntity<ApiResponse<CategorySummaryResponse>> create(
            @Valid @RequestBody CategoryRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(categoryCommandService.create(request)));
    }

    @Operation(summary = "List categories — summaries only, no unit policy")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CategorySummaryResponse>>> list(
            @RequestParam(required = false) String search,
            @ModelAttribute PageableRequest pageable) {

        return ResponseEntity.ok(ApiResponse.ok(categoryQueryService.findAll(search, pageable.toPageable())));
    }

    @Operation(summary = "Every category, unpaged — for the pickers that choose one")
    @GetMapping("/selection")
    public ResponseEntity<ApiResponse<List<CategorySummaryResponse>>> selection() {
        return ResponseEntity.ok(ApiResponse.ok(categoryQueryService.findAllForSelection()));
    }

    @Operation(summary = "One category, with its permitted units and product count")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryQueryService.findById(id)));
    }

    @Operation(summary = "Edit a category")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategorySummaryResponse>> update(
            @PathVariable Long id, @Valid @RequestBody CategoryRequest request) {

        return ResponseEntity.ok(ApiResponse.ok("Category updated", categoryCommandService.update(id, request)));
    }

    // ── Unit policy ───────────────────────────────────────────────────────

    @Operation(summary = "Permit a unit for buying or selling in this category")
    @PostMapping("/{id}/units")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> allowUnit(
            @PathVariable Long id, @Valid @RequestBody CategoryUnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(categoryCommandService.allowUnit(id, request)));
    }

    @Operation(summary = "The units this category permits for one side of the trade")
    @GetMapping("/{id}/units")
    public ResponseEntity<ApiResponse<List<UnitResponse>>> permittedUnits(
            @PathVariable Long id, @RequestParam UnitUsage usage) {

        return ResponseEntity.ok(ApiResponse.ok(categoryQueryService.findPermittedUnits(id, usage)));
    }

    @Operation(summary = "Withdraw a unit permission")
    @DeleteMapping("/{id}/units/{categoryUnitId}")
    public ResponseEntity<ApiResponse<Void>> revokeUnit(
            @PathVariable Long id, @PathVariable Long categoryUnitId) {

        categoryCommandService.revokeUnit(id, categoryUnitId);
        return ResponseEntity.ok(ApiResponse.ok("Unit permission withdrawn"));
    }
}
