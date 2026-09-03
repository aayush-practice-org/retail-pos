package io.aygh.stock.service.query.impl;

import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import io.aygh.stock.dto.response.ProductStockResponse;
import io.aygh.stock.dto.response.StockMovementResponse;
import io.aygh.stock.dto.response.StockOverviewResponse;
import io.aygh.stock.entity.ProductStock;
import io.aygh.stock.entity.StockMovement;
import io.aygh.stock.entity.StockReferenceType;
import io.aygh.stock.helper.StockResolver;
import io.aygh.stock.mapper.StockMapper;
import io.aygh.stock.repository.ProductStockRepository;
import io.aygh.stock.repository.StockMovementRepository;
import io.aygh.stock.service.query.StockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockQueryServiceImpl implements StockQueryService {

    private final ProductStockRepository productStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockResolver resolver;
    private final StockMapper stockMapper;

    @Override
    public PagedResponse<ProductStockResponse> findLevels(String search, Long categoryId,
                                                          boolean lowOnly, Pageable pageable) {
        String pattern = (search == null || search.isBlank())
                ? null : "%" + search.trim().toLowerCase() + "%";

        Page<ProductStock> page = productStockRepository.search(pattern, categoryId, lowOnly, pageable);
        return PaginationUtils.toPagedResponse(page, page.map(stockMapper::toResponse).getContent());
    }

    /**
     * A product that has never moved has no row; the resolver opens one at zero
     * rather than 404-ing, because "none on hand" is the honest answer.
     */
    @Override
    public ProductStockResponse findByProductId(Long productId) {
        resolver.product(productId);
        return stockMapper.toResponse(resolver.stockOf(productId));
    }

    @Override
    public PagedResponse<StockMovementResponse> findMovements(Long productId,
                                                              StockReferenceType referenceType,
                                                              Pageable pageable) {
        if (productId != null) {
            resolver.product(productId);
        }

        Page<StockMovement> page = stockMovementRepository.search(productId, referenceType, pageable);
        return PaginationUtils.toPagedResponse(page, page.map(stockMapper::toResponse).getContent());
    }

    @Override
    public StockOverviewResponse overview() {
        return new StockOverviewResponse(
                productStockRepository.count(),
                productStockRepository.countNeedingAttention());
    }
}
