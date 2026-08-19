package io.aygh.catalog.service.query.impl;

import io.aygh.catalog.dto.response.ProductVariantResponse;
import io.aygh.catalog.entity.Product;
import io.aygh.catalog.entity.ProductVariant;
import io.aygh.catalog.helper.ProductResolver;
import io.aygh.catalog.helper.ProductVariantResolver;
import io.aygh.catalog.mapper.ProductVariantMapper;
import io.aygh.catalog.repository.ProductVariantRepository;
import io.aygh.catalog.service.query.ProductVariantQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ProductVariantQueryServiceImpl implements ProductVariantQueryService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductResolver productResolver;
    private final ProductVariantResolver productVariantResolver;
    private final ProductVariantMapper productVariantMapper;

    @Override
    public List<ProductVariantResponse> findByProduct(Long productId) {
        Product product = productResolver.resolveWithCategoryAndUnit(productId);

        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId);

        return productVariantMapper.toResponses(variants, product.getBaseUnit().getSymbol());
    }

    @Override
    public ProductVariantResponse findById(Long productId, Long variantId) {
        Product product = productResolver.resolveWithCategoryAndUnit(productId);
        ProductVariant variant = productVariantResolver.resolve(productId, variantId);

        return productVariantMapper.toResponse(variant, product.getBaseUnit().getSymbol());
    }
}
