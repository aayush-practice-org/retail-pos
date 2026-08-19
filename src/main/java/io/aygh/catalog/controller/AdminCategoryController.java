package io.aygh.catalog.controller;

import io.aygh.catalog.dto.request.CategoryRequest;
import io.aygh.catalog.dto.response.CategoryResponse;
import io.aygh.catalog.service.command.CategoryCommandService;
import io.aygh.catalog.service.query.CategoryQueryService;
import io.aygh.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 *  Admin Category controller
 *
 *  Categories are hierarchical — a category may hold subcategories to any depth.
 *  Fetched whole (NO pagination), since the tree is browsed rather than paged.
 */
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryController {

    private final CategoryCommandService categoryCommandService;
    private final CategoryQueryService categoryQueryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        log.info("ADMIN REST request to create Category: {}", request.name());
        CategoryResponse response = categoryCommandService.create(request);
        return ResponseEntity.ok(ApiResponse.ok("Category created successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        log.info("ADMIN REST request to update Category: {}, {}", id, request.name());
        CategoryResponse response = categoryCommandService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Category updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("ADMIN REST request to delete Category: {}", id);
        categoryCommandService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Category deleted successfully", null));
    }

    /**
     * The whole tree: root categories with their subcategories nested underneath.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> findAll() {
        log.info("ADMIN REST request to get all Categories");
        List<CategoryResponse> list = categoryQueryService.findAll();
        return ResponseEntity.ok(ApiResponse.ok("Categories fetched successfully", list));
    }

    /**
     * Root categories only, without their subtrees — for pickers and breadcrumbs.
     */
    @GetMapping("/roots")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> findRoots() {
        log.info("ADMIN REST request to get root Categories");
        List<CategoryResponse> list = categoryQueryService.findRoots();
        return ResponseEntity.ok(ApiResponse.ok("Categories fetched successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> findById(@PathVariable Long id) {
        log.info("ADMIN REST request to get Category by id: {}", id);
        CategoryResponse response = categoryQueryService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok("Category fetched successfully", response));
    }
}
