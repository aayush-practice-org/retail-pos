package io.aygh.catalog.service.command.impl;

import io.aygh.catalog.dto.request.ProductRequest;
import io.aygh.catalog.dto.response.ProductResponse;
import io.aygh.catalog.entity.Category;
import io.aygh.catalog.entity.Product;
import io.aygh.catalog.helper.ProductResolver;
import io.aygh.catalog.helper.ProductValidation;
import io.aygh.catalog.mapper.ProductMapper;
import io.aygh.catalog.repository.ProductRepository;
import io.aygh.catalog.repository.ProductVariantRepository;
import io.aygh.catalog.service.command.ProductCommandService;
import io.aygh.exception.BusinessException;
import io.aygh.unit.entity.SystemUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
class ProductCommandServiceImpl implements ProductCommandService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductResolver productResolver;
    private final ProductValidation productValidation;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductResponse create(ProductRequest request) {
        log.info("Creating product: {}", request.name());

        productValidation.validateUniqueName(request.name(), null);

        Category category = productResolver.resolveCategory(request.categoryId());
        SystemUnit baseUnit = productResolver.resolveBaseUnit(request.baseUnitId());

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .brand(request.brand())
                .category(category)
                .baseUnit(baseUnit)
                .imageUrl(request.imageUrl())
                .isActive(request.isActive())
                .build();

        return productMapper.toResponse(productRepository.save(product), 0);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductResponse update(Long id, ProductRequest request) {
        log.info("Updating product id: {}", id);

        Product product = productResolver.resolveWithCategoryAndUnit(id);

        productValidation.validateUniqueName(request.name(), id);

        Category category = productResolver.resolveCategory(request.categoryId());
        SystemUnit baseUnit = productResolver.resolveBaseUnit(request.baseUnitId());

        long variantCount = productVariantRepository.countByProductId(id);
        validateBaseUnitChange(product, baseUnit, variantCount);

        product.setName(request.name());
        product.setDescription(request.description());
        product.setBrand(request.brand());
        product.setCategory(category);
        product.setBaseUnit(baseUnit);
        product.setImageUrl(request.imageUrl());
        product.setActive(request.isActive());

        return productMapper.toResponse(productRepository.save(product), variantCount);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public void delete(Long id) {
        Product product = productResolver.resolve(id);
        productRepository.delete(product);
        log.info("Deleted product id={} (and its variants)", id);
    }

    /**
     * Every variant's pack size is stored in the product's base unit, so swapping
     * that unit underneath existing variants would silently reinterpret all of them.
     */
    private void validateBaseUnitChange(Product product, SystemUnit newBaseUnit, long variantCount) {
        if (product.getBaseUnit().getId().equals(newBaseUnit.getId()) || variantCount == 0) {
            return;
        }

        throw new BusinessException(
                "Cannot change the base unit of '%s' from %s to %s while it has %d variant(s) — remove them first"
                        .formatted(product.getName(), product.getBaseUnit().getSymbol(),
                                newBaseUnit.getSymbol(), variantCount));
    }
}
