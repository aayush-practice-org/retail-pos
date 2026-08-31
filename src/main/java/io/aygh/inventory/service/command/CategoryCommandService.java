package io.aygh.inventory.service.command;

import io.aygh.inventory.dto.request.CategoryRequest;
import io.aygh.inventory.dto.request.CategoryUnitRequest;
import io.aygh.inventory.dto.response.CategoryDetailResponse;
import io.aygh.inventory.dto.response.CategorySummaryResponse;

public interface CategoryCommandService {

    CategorySummaryResponse create(CategoryRequest request);

    CategorySummaryResponse update(Long id, CategoryRequest request);

    void delete(Long id);

    /**
     * Authorises one unit for one side of the trade in this category.
     */
    CategoryDetailResponse allowUnit(Long categoryId, CategoryUnitRequest request);

    void revokeUnit(Long categoryId, Long categoryUnitId);
}
