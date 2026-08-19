package io.aygh.catalog.service.query;

import io.aygh.catalog.dto.response.ProductDetailResponse;
import io.aygh.catalog.dto.response.ProductResponse;
import io.aygh.shared.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface ProductQueryService {

    PagedResponse<ProductResponse> findAll(String search,
                                           Long categoryId,
                                           Boolean isActive,
                                           boolean includeSubCategories,
                                           Pageable pageable);

    ProductDetailResponse findById(Long id);
}
