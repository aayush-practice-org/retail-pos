package io.aygh.inventory.service.command;

import io.aygh.inventory.dto.request.CategoryRequest;
import io.aygh.inventory.dto.request.CategoryUnitRequest;
import io.aygh.inventory.dto.response.CategoryDetailResponse;
import io.aygh.inventory.dto.response.CategorySummaryResponse;

public interface CategoryCommandService {

    CategorySummaryResponse create(CategoryRequest request);

    CategorySummaryResponse update(Long id, CategoryRequest request);

    /**
     * Makes this the category products land in when nobody chose one — what
     * quick-add at the till and an import with a blank category column use.
     * Whichever category held it loses it, so exactly one always has it.
     */
    CategorySummaryResponse makeDefault(Long id);

    void delete(Long id);

    /**
     * Authorises one unit for one side of the trade in this category.
     */
    CategoryDetailResponse allowUnit(Long categoryId, CategoryUnitRequest request);

    void revokeUnit(Long categoryId, Long categoryUnitId);
}
