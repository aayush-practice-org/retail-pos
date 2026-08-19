package io.aygh.catalog.service.command;

import io.aygh.catalog.dto.request.ProductVariantRequest;
import io.aygh.catalog.dto.response.ProductVariantResponse;

public interface ProductVariantCommandService {

    ProductVariantResponse create(Long productId, ProductVariantRequest request);

    ProductVariantResponse update(Long productId, Long variantId, ProductVariantRequest request);

    ProductVariantResponse setDefault(Long productId, Long variantId);

    void delete(Long productId, Long variantId);
}
