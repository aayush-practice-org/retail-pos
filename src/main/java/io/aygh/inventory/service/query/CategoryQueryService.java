package io.aygh.inventory.service.query;

import io.aygh.inventory.dto.response.CategoryDetailResponse;
import io.aygh.inventory.dto.response.CategorySummaryResponse;
import io.aygh.inventory.dto.response.UnitResponse;
import io.aygh.inventory.entity.UnitUsage;
import io.aygh.shared.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryQueryService {

    /** The full view: unit policy and product count included. */
    CategoryDetailResponse findById(Long id);

    PagedResponse<CategorySummaryResponse> findAll(String search, Pageable pageable);

    List<CategorySummaryResponse> findAllForSelection();

    /** What a product in this category may be configured with, for the form's dropdown. */
    List<UnitResponse> findPermittedUnits(Long categoryId, UnitUsage usage);
}
