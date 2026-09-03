package io.aygh.stock.service.query;

import io.aygh.shared.response.PagedResponse;
import io.aygh.stock.dto.response.ProductStockResponse;
import io.aygh.stock.dto.response.StockMovementResponse;
import io.aygh.stock.dto.response.StockOverviewResponse;
import io.aygh.stock.entity.StockReferenceType;
import org.springframework.data.domain.Pageable;

public interface StockQueryService {

    /** Stock levels, optionally narrowed to a category, a search, or only what needs attention. */
    PagedResponse<ProductStockResponse> findLevels(String search, Long categoryId,
                                                   boolean lowOnly, Pageable pageable);

    ProductStockResponse findByProductId(Long productId);

    PagedResponse<StockMovementResponse> findMovements(Long productId,
                                                       StockReferenceType referenceType,
                                                       Pageable pageable);

    StockOverviewResponse overview();
}
