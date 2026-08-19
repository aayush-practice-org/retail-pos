package io.aygh.catalog.service.query.impl;

import io.aygh.catalog.dto.response.ProductDetailResponse;
import io.aygh.catalog.dto.response.ProductResponse;
import io.aygh.catalog.entity.Product;
import io.aygh.catalog.entity.ProductVariant;
import io.aygh.catalog.helper.CategoryResolver;
import io.aygh.catalog.helper.ProductResolver;
import io.aygh.catalog.mapper.ProductMapper;
import io.aygh.catalog.mapper.ProductVariantMapper;
import io.aygh.catalog.repository.ProductRepository;
import io.aygh.catalog.repository.ProductVariantRepository;
import io.aygh.catalog.service.query.ProductQueryService;
import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ProductQueryServiceImpl implements ProductQueryService {

    /** Bound for the IN clause when no category filter is applied — never matches. */
    private static final Collection<Long> NO_CATEGORY_FILTER = List.of(-1L);

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductResolver productResolver;
    private final CategoryResolver categoryResolver;
    private final ProductMapper productMapper;
    private final ProductVariantMapper productVariantMapper;

    @Override
    @Cacheable(
            cacheNames = "products",
            key = "{#search, #categoryId, #isActive, #includeSubCategories, #pageable}"
    )
    public PagedResponse<ProductResponse> findAll(String search,
                                                  Long categoryId,
                                                  Boolean isActive,
                                                  boolean includeSubCategories,
                                                  Pageable pageable) {

        Collection<Long> categoryIds = resolveCategoryFilter(categoryId, includeSubCategories);

        Page<Product> products = productRepository.search(
                search, categoryId == null, categoryIds, isActive, pageable);

        Map<Long, Long> variantCounts = countVariants(products.getContent());

        List<ProductResponse> content = products.getContent().stream()
                .map(product -> productMapper.toResponse(
                        product, variantCounts.getOrDefault(product.getId(), 0L)))
                .toList();

        return PaginationUtils.toPagedResponse(products, content);
    }

    @Override
    public ProductDetailResponse findById(Long id) {
        Product product = productResolver.resolveWithCategoryAndUnit(id);

        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByCreatedAtAsc(id);

        return productMapper.toDetailResponse(
                product,
                productVariantMapper.toResponses(variants, product.getBaseUnit().getSymbol()));
    }

    private Collection<Long> resolveCategoryFilter(Long categoryId, boolean includeSubCategories) {
        if (categoryId == null) {
            return NO_CATEGORY_FILTER;
        }

        // makes sure the category actually exists before filtering on it
        categoryResolver.resolve(categoryId);

        return includeSubCategories
                ? categoryResolver.collectSubTreeIds(categoryId)
                : Set.of(categoryId);
    }

    /**
     * One grouped query for the whole page instead of a count per product.
     */
    private Map<Long, Long> countVariants(List<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();

        return productVariantRepository.countByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(
                        ProductVariantRepository.VariantCount::getProductId,
                        ProductVariantRepository.VariantCount::getTotal));
    }
}
