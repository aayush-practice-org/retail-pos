package io.aygh.inventory.service.query;

import io.aygh.inventory.dto.response.ProductDetailResponse;
import io.aygh.inventory.dto.response.ProductPurchaseUnitDetailResponse;
import io.aygh.inventory.dto.response.ProductPurchaseUnitResponse;
import io.aygh.inventory.dto.response.ProductSellingUnitResponse;
import io.aygh.inventory.dto.response.ProductSummaryResponse;
import io.aygh.shared.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductQueryService {

    /**
     * The list shape: no description, no trading configuration. Kept apart from
     * {@link #findById} so a page of twenty rows stays one query.
     */
    PagedResponse<ProductSummaryResponse> findAll(String search, Long categoryId, Boolean active, Pageable pageable);

    ProductDetailResponse findById(Long id);

    List<ProductPurchaseUnitResponse> findPurchaseUnits(Long productId);

    ProductPurchaseUnitDetailResponse findPurchaseUnit(Long productId, Long purchaseUnitId);

    List<ProductSellingUnitResponse> findSellingUnits(Long productId);

    /** What the till resolves a scan to. */
    ProductSellingUnitResponse findByBarcode(String barcode);
}
