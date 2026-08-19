package io.aygh.catalog.service.command.impl;

import io.aygh.catalog.dto.request.ProductVariantRequest;
import io.aygh.catalog.dto.response.ProductVariantResponse;
import io.aygh.catalog.entity.Product;
import io.aygh.catalog.entity.ProductVariant;
import io.aygh.catalog.helper.ProductResolver;
import io.aygh.catalog.helper.ProductVariantResolver;
import io.aygh.catalog.helper.ProductVariantValidation;
import io.aygh.catalog.mapper.ProductVariantMapper;
import io.aygh.catalog.repository.ProductVariantRepository;
import io.aygh.catalog.service.command.ProductVariantCommandService;
import io.aygh.exception.BusinessException;
import io.aygh.unit.entity.SystemUnit;
import io.aygh.unit.entity.UnitSource;
import io.aygh.unit.service.UnitConversionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/*
 *  Variants belong to one product and are only ever created, edited and removed
 *  through it — nothing here is shared with, or reachable from, another product.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class ProductVariantCommandServiceImpl implements ProductVariantCommandService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductResolver productResolver;
    private final ProductVariantResolver productVariantResolver;
    private final ProductVariantValidation productVariantValidation;
    private final ProductVariantMapper productVariantMapper;
    private final UnitConversionService unitConversionService;

    @Override
    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductVariantResponse create(Long productId, ProductVariantRequest request) {
        log.info("Creating variant '{}' on product id: {}", request.name(), productId);

        Product product = productResolver.resolveWithCategoryAndUnit(productId);

        productVariantValidation.validateUniqueName(productId, request.name(), null);
        productVariantValidation.validateUniqueSku(request.sku(), null);
        productVariantValidation.validateUniqueBarcode(request.barcode(), null);
        productVariantValidation.validatePricing(request.mrp(), request.sellingPrice());

        // the first variant of a product is always the default one
        boolean makeDefault = request.isDefault() || productVariantRepository.countByProductId(productId) == 0;
        if (makeDefault) {
            demoteCurrentDefaults(productId, null);
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .name(request.name())
                .sku(blankToNull(request.sku()))
                .barcode(blankToNull(request.barcode()))
                .packSize(toBaseUnit(request, product.getBaseUnit()))
                .originalQuantity(request.packUnitId() == null ? null : request.packSize())
                .originalUnitId(request.packUnitId())
                .originalUnitSource(request.packUnitId() == null ? null : sourceOf(request))
                .mrp(request.mrp())
                .sellingPrice(request.sellingPrice())
                .isDefault(makeDefault)
                .isActive(request.isActive())
                .build();

        ProductVariant saved = productVariantRepository.save(variant);
        log.info("Created variant id={} on product id={}", saved.getId(), productId);

        return productVariantMapper.toResponse(saved, product.getBaseUnit().getSymbol());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductVariantResponse update(Long productId, Long variantId, ProductVariantRequest request) {
        log.info("Updating variant id: {} on product id: {}", variantId, productId);

        Product product = productResolver.resolveWithCategoryAndUnit(productId);
        ProductVariant variant = productVariantResolver.resolve(productId, variantId);

        productVariantValidation.validateUniqueName(productId, request.name(), variantId);
        productVariantValidation.validateUniqueSku(request.sku(), variantId);
        productVariantValidation.validateUniqueBarcode(request.barcode(), variantId);
        productVariantValidation.validatePricing(request.mrp(), request.sellingPrice());

        variant.setName(request.name());
        variant.setSku(blankToNull(request.sku()));
        variant.setBarcode(blankToNull(request.barcode()));
        variant.setPackSize(toBaseUnit(request, product.getBaseUnit()));
        variant.setOriginalQuantity(request.packUnitId() == null ? null : request.packSize());
        variant.setOriginalUnitId(request.packUnitId());
        variant.setOriginalUnitSource(request.packUnitId() == null ? null : sourceOf(request));
        variant.setMrp(request.mrp());
        variant.setSellingPrice(request.sellingPrice());
        variant.setActive(request.isActive());

        applyDefaultFlag(productId, variant, request.isDefault());

        return productVariantMapper.toResponse(variant, product.getBaseUnit().getSymbol());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductVariantResponse setDefault(Long productId, Long variantId) {
        log.info("Setting variant id: {} as default on product id: {}", variantId, productId);

        Product product = productResolver.resolveWithCategoryAndUnit(productId);
        ProductVariant variant = productVariantResolver.resolve(productId, variantId);

        demoteCurrentDefaults(productId, variantId);
        variant.setDefault(true);

        return productVariantMapper.toResponse(variant, product.getBaseUnit().getSymbol());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public void delete(Long productId, Long variantId) {
        ProductVariant variant = productVariantResolver.resolve(productId, variantId);

        // pick the successor before the row goes away, so the product keeps a default
        Optional<ProductVariant> successor = variant.isDefault()
                ? findSuccessor(productId, variantId)
                : Optional.empty();

        productVariantRepository.delete(variant);
        successor.ifPresent(next -> next.setDefault(true));

        log.info("Deleted variant id={} from product id={}", variantId, productId);
    }

    // ── internals ─────────────────────────────────────────────────────────

    /**
     * Pack sizes are stored in the product's base unit. When the operator entered
     * the size in another unit ("1 kg" for a product tracked in grams) it is
     * converted here, and what they typed is kept alongside for display.
     */
    private BigDecimal toBaseUnit(ProductVariantRequest request, SystemUnit baseUnit) {
        return unitConversionService.convertToUnit(
                request.packSize(), request.packUnitId(), sourceOf(request), baseUnit);
    }

    private UnitSource sourceOf(ProductVariantRequest request) {
        return request.packUnitSource() == null ? UnitSource.SYSTEM : request.packUnitSource();
    }

    private void applyDefaultFlag(Long productId, ProductVariant variant, boolean requestedDefault) {
        if (requestedDefault) {
            demoteCurrentDefaults(productId, variant.getId());
            variant.setDefault(true);
            return;
        }

        if (!variant.isDefault()) {
            return;
        }

        // the only variant of a product stays its default whatever the payload says
        if (productVariantRepository.countByProductId(productId) <= 1) {
            variant.setDefault(true);
            return;
        }

        throw new BusinessException(
                "'%s' is the default variant — mark another variant as default instead of unsetting it"
                        .formatted(variant.getName()));
    }

    private void demoteCurrentDefaults(Long productId, Long excludeVariantId) {
        for (ProductVariant current : productVariantRepository.findDefaultsByProductId(productId)) {
            if (excludeVariantId == null || !excludeVariantId.equals(current.getId())) {
                current.setDefault(false);
            }
        }
    }

    private Optional<ProductVariant> findSuccessor(Long productId, Long variantId) {
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId);

        return variants.stream()
                .filter(candidate -> !candidate.getId().equals(variantId))
                .findFirst();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
