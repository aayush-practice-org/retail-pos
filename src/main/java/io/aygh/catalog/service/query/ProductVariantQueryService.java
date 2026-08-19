package io.aygh.catalog.service.query;

import io.aygh.catalog.dto.response.ProductVariantResponse;

import java.util.List;

public interface ProductVariantQueryService {

    List<ProductVariantResponse> findByProduct(Long productId);

    ProductVariantResponse findById(Long productId, Long variantId);
}
