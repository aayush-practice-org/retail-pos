package io.aygh.inventory.service.command.impl;

import io.aygh.inventory.dto.request.ProductCreateRequest;
import io.aygh.inventory.dto.request.ProductPurchaseUnitCreateRequest;
import io.aygh.inventory.dto.request.ProductPurchaseUnitUpdateRequest;
import io.aygh.inventory.dto.request.ProductSellingUnitCreateRequest;
import io.aygh.inventory.dto.request.ProductSellingUnitUpdateRequest;
import io.aygh.inventory.dto.request.ProductUpdateRequest;
import io.aygh.inventory.dto.request.ProductVatRequest;
import io.aygh.inventory.dto.response.ProductDetailResponse;
import io.aygh.inventory.dto.response.ProductPurchaseUnitDetailResponse;
import io.aygh.inventory.dto.response.ProductSellingUnitResponse;
import io.aygh.inventory.dto.response.ProductSummaryResponse;
import io.aygh.inventory.entity.Category;
import io.aygh.inventory.entity.Product;
import io.aygh.inventory.entity.ProductPurchaseUnit;
import io.aygh.inventory.entity.ProductPurchaseVat;
import io.aygh.inventory.entity.ProductSellingUnit;
import io.aygh.inventory.entity.Unit;
import io.aygh.inventory.entity.UnitUsage;
import io.aygh.inventory.helper.InventoryResolver;
import io.aygh.inventory.helper.InventoryValidation;
import io.aygh.inventory.mapper.ProductMapper;
import io.aygh.inventory.mapper.ProductUnitMapper;
import io.aygh.inventory.repository.ProductPurchaseUnitRepository;
import io.aygh.inventory.repository.ProductPurchaseVatRepository;
import io.aygh.inventory.repository.ProductRepository;
import io.aygh.inventory.repository.ProductSellingUnitRepository;
import io.aygh.inventory.service.command.ProductCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductCommandServiceImpl implements ProductCommandService {

    private final ProductRepository productRepository;
    private final ProductPurchaseUnitRepository purchaseUnitRepository;
    private final ProductSellingUnitRepository sellingUnitRepository;
    private final ProductPurchaseVatRepository vatRepository;
    private final InventoryResolver resolver;
    private final InventoryValidation validation;
    private final ProductMapper productMapper;
    private final ProductUnitMapper productUnitMapper;

    // ── Product ───────────────────────────────────────────────────────────

    @Override
    public ProductSummaryResponse create(ProductCreateRequest request) {

        Category category = resolver.category(request.getCategoryId());
        Unit baseUnit = resolver.unit(request.getBaseUnitId());

        validation.requireUsableAsBaseUnit(baseUnit);
        validation.requireProductNameAvailable(request.getName(), category.getId(), null);
        validation.requireProductCodeAvailable(request.getProductCode(), null);

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setBaseUnit(baseUnit);

        Product saved = productRepository.save(product);
        log.info("Created product '{}' in category '{}', counted in {}",
                saved.getName(), category.getName(), baseUnit.getSymbol());
        return productMapper.toSummary(saved);
    }

    @Override
    public ProductSummaryResponse update(Long id, ProductUpdateRequest request) {
        Product product = resolver.product(id);

        validation.requireProductNameAvailable(request.getName(), product.getCategory().getId(), id);
        validation.requireProductCodeAvailable(request.getProductCode(), id);

        productMapper.applyUpdate(request, product);

        Product saved = productRepository.save(product);
        log.info("Updated product '{}'", saved.getName());
        return productMapper.toSummary(saved);
    }

    @Override
    public void delete(Long id) {
        Product product = resolver.product(id);

        productRepository.delete(product);
        log.info("Retired product '{}'", product.getName());
    }

    // ── How the product is bought ─────────────────────────────────────────

    @Override
    public ProductPurchaseUnitDetailResponse addPurchaseUnit(
            Long productId, ProductPurchaseUnitCreateRequest request) {

        Product product = resolver.product(productId);
        Unit unit = resolver.unit(request.getUnitId());

        validation.requirePermittedByCategory(product.getCategory(), unit, UnitUsage.PURCHASE);
        validation.requireSameMeasurement(product.getBaseUnit(), unit);
        validation.requirePurchaseUnitNotConfigured(productId, unit.getId(), null);

        ProductPurchaseUnit purchaseUnit = productUnitMapper.toEntity(request);
        purchaseUnit.setProduct(product);
        purchaseUnit.setUnit(unit);

        if (purchaseUnit.isDefault()) {
            purchaseUnitRepository.clearDefaults(productId);
        }

        ProductPurchaseUnit saved = purchaseUnitRepository.save(purchaseUnit);

        if (request.getVat() != null) {
            openVatRate(saved, request.getVat());
        }

        log.info("Product '{}' can now be bought by the {}", product.getName(), unit.getSymbol());
        return productUnitMapper.toDetail(resolver.purchaseUnitDetail(productId, saved.getId()));
    }

    @Override
    public ProductPurchaseUnitDetailResponse updatePurchaseUnit(
            Long productId, Long purchaseUnitId, ProductPurchaseUnitUpdateRequest request) {

        ProductPurchaseUnit purchaseUnit = resolver.purchaseUnit(productId, purchaseUnitId);

        boolean becomingDefault = request.isDefault() && !purchaseUnit.isDefault();
        productUnitMapper.applyUpdate(request, purchaseUnit);

        if (becomingDefault) {
            purchaseUnitRepository.clearDefaults(productId);
            purchaseUnit.setDefault(true);
        }

        purchaseUnitRepository.save(purchaseUnit);
        return productUnitMapper.toDetail(resolver.purchaseUnitDetail(productId, purchaseUnitId));
    }

    @Override
    public void removePurchaseUnit(Long productId, Long purchaseUnitId) {
        ProductPurchaseUnit purchaseUnit = resolver.purchaseUnit(productId, purchaseUnitId);

        purchaseUnitRepository.delete(purchaseUnit);
        log.info("Removed purchase unit {} from product {}", purchaseUnitId, productId);
    }

    @Override
    public ProductPurchaseUnitDetailResponse addVatRate(
            Long productId, Long purchaseUnitId, ProductVatRequest request) {

        ProductPurchaseUnit purchaseUnit = resolver.purchaseUnit(productId, purchaseUnitId);

        openVatRate(purchaseUnit, request);
        return productUnitMapper.toDetail(resolver.purchaseUnitDetail(productId, purchaseUnitId));
    }

    /**
     * Closes whichever rate is still open the day before the new one starts, then
     * opens the new one. Two open-ended rates on the same purchase unit would
     * make "what is the rate today" ambiguous, and nothing in the schema forbids
     * it — so it is forbidden here, on the one path that creates them.
     */
    private void openVatRate(ProductPurchaseUnit purchaseUnit, ProductVatRequest request) {
        LocalDate from = request.effectiveFrom() == null ? LocalDate.now() : request.effectiveFrom();

        vatRepository.findByProductPurchaseUnitIdAndEffectiveToIsNull(purchaseUnit.getId())
                .ifPresent(current -> {
                    current.setEffectiveTo(from.minusDays(1));
                    vatRepository.save(current);
                });

        vatRepository.save(ProductPurchaseVat.builder()
                .productPurchaseUnit(purchaseUnit)
                .rate(request.rate())
                .effectiveFrom(from)
                .build());

        log.info("VAT on purchase unit {} is {}% from {}", purchaseUnit.getId(), request.rate(), from);
    }

    // ── How the product is sold ───────────────────────────────────────────

    @Override
    public ProductSellingUnitResponse addSellingUnit(Long productId, ProductSellingUnitCreateRequest request) {
        Product product = resolver.product(productId);
        Unit unit = resolver.unit(request.getUnitId());

        validation.requirePermittedByCategory(product.getCategory(), unit, UnitUsage.SELLING);
        validation.requireSameMeasurement(product.getBaseUnit(), unit);
        validation.requireSellingUnitNotConfigured(productId, unit.getId(), null);
        validation.requireBarcodeAvailable(request.getBarcode(), null);
        validation.requireSkuAvailable(request.getSku(), null);

        ProductSellingUnit sellingUnit = productUnitMapper.toEntity(request);
        sellingUnit.setProduct(product);
        sellingUnit.setUnit(unit);

        if (sellingUnit.isDefault()) {
            sellingUnitRepository.clearDefaults(productId);
        }

        ProductSellingUnit saved = sellingUnitRepository.save(sellingUnit);
        log.info("Product '{}' can now be sold by the {}", product.getName(), unit.getSymbol());
        return productUnitMapper.toResponse(saved);
    }

    @Override
    public ProductSellingUnitResponse updateSellingUnit(
            Long productId, Long sellingUnitId, ProductSellingUnitUpdateRequest request) {

        ProductSellingUnit sellingUnit = resolver.sellingUnit(productId, sellingUnitId);

        validation.requireBarcodeAvailable(request.getBarcode(), sellingUnitId);
        validation.requireSkuAvailable(request.getSku(), sellingUnitId);

        boolean becomingDefault = request.isDefault() && !sellingUnit.isDefault();
        productUnitMapper.applyUpdate(request, sellingUnit);

        if (becomingDefault) {
            sellingUnitRepository.clearDefaults(productId);
            sellingUnit.setDefault(true);
        }

        return productUnitMapper.toResponse(sellingUnitRepository.save(sellingUnit));
    }

    @Override
    public void removeSellingUnit(Long productId, Long sellingUnitId) {
        ProductSellingUnit sellingUnit = resolver.sellingUnit(productId, sellingUnitId);

        sellingUnitRepository.delete(sellingUnit);
        log.info("Removed selling unit {} from product {}", sellingUnitId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse reload(Long productId) {
        return productMapper.toDetail(resolver.productDetail(productId));
    }
}
