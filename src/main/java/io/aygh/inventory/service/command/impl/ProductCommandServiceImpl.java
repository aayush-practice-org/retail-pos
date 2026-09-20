package io.aygh.inventory.service.command.impl;

import io.aygh.config.properties.TaxProperties;
import io.aygh.exception.BusinessException;
import io.aygh.inventory.dto.request.ProductCreateRequest;
import io.aygh.inventory.dto.request.ProductPurchaseUnitCreateRequest;
import io.aygh.inventory.dto.request.ProductPurchaseUnitUpdateRequest;
import io.aygh.inventory.dto.request.ProductSellingUnitCreateRequest;
import io.aygh.inventory.dto.request.ProductSellingUnitUpdateRequest;
import io.aygh.inventory.dto.request.ProductUnitLineRequest;
import io.aygh.inventory.dto.request.ProductUpdateRequest;
import io.aygh.inventory.dto.request.ProductVatRequest;
import io.aygh.inventory.dto.request.QuickAddRequest;
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
import io.aygh.inventory.entity.MeasurementType;
import io.aygh.inventory.helper.CategoryUnitGrants;
import io.aygh.inventory.helper.InventoryResolver;
import io.aygh.inventory.helper.InventoryValidation;
import io.aygh.inventory.helper.ProductUnitFactory;
import io.aygh.inventory.mapper.ProductMapper;
import io.aygh.inventory.mapper.ProductUnitMapper;
import io.aygh.inventory.repository.ProductPurchaseUnitRepository;
import io.aygh.inventory.repository.ProductPurchaseVatRepository;
import io.aygh.inventory.repository.CategoryRepository;
import io.aygh.inventory.repository.ProductRepository;
import io.aygh.inventory.repository.ProductSellingUnitRepository;
import io.aygh.inventory.service.command.ProductCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductCommandServiceImpl implements ProductCommandService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductPurchaseUnitRepository purchaseUnitRepository;
    private final ProductSellingUnitRepository sellingUnitRepository;
    private final ProductPurchaseVatRepository vatRepository;
    private final InventoryResolver resolver;
    private final InventoryValidation validation;
    private final ProductUnitFactory unitFactory;
    private final CategoryUnitGrants grants;
    private final TaxProperties taxProperties;
    private final ProductMapper productMapper;
    private final ProductUnitMapper productUnitMapper;

    // ── Product ───────────────────────────────────────────────────────────

    /**
     * The product and everything it needs to be bought and sold, in one
     * transaction. Either the whole shelf entry exists or none of it does —
     * which the four-call version could not promise, and a product stranded
     * halfway with no price is one the till cannot ring up.
     */
    @Override
    public ProductDetailResponse create(ProductCreateRequest request) {

        Category category = resolver.category(request.getCategoryId());

        List<ProductUnitLineRequest> sellLines = sellLines(request);
        List<ProductUnitLineRequest> buyLines = buyLines(request);

        Unit baseUnit = baseUnitFor(request, sellLines, buyLines);
        validation.requireUsableAsBaseUnit(baseUnit);
        validation.requireProductNameAvailable(request.getName(), category.getId(), null);
        validation.requireProductCodeAvailable(request.getProductCode(), null);

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setBaseUnit(baseUnit);

        Product saved = productRepository.save(product);

        applySellLines(saved, sellLines);
        applyBuyLines(saved, buyLines);

        productRepository.flush();
        log.info("Created product '{}' in category '{}', counted in {} ({} selling, {} purchase unit(s))",
                saved.getName(), category.getName(), baseUnit.getSymbol(),
                sellLines.size(), buyLines.size());
        return reload(saved.getId());
    }

    /**
     * The till's path into the catalogue. Deliberately a thin wrapper over
     * {@link #create}: a second way to bring a product into existence would be
     * a second place for the rules about units, defaults and VAT to drift.
     */
    @Override
    public ProductSellingUnitResponse quickAdd(QuickAddRequest request) {
        ProductCreateRequest create = new ProductCreateRequest();
        create.setName(request.name());
        create.setCategoryId(request.categoryId() == null ? defaultCategoryId() : request.categoryId());
        create.setBarcode(request.barcode());
        create.setSellingPrice(request.sellingPrice());
        create.setPurchasePrice(request.purchasePrice());

        ProductDetailResponse created = create(create);

        // Answers with the same shape a successful scan would have, so the
        // caller splices it straight into the sale it was in the middle of.
        return created.getSellingUnits().stream()
                .filter(ProductSellingUnitResponse::isDefault)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "'" + request.name() + "' was created but has no sellable unit"));
    }

    private Long defaultCategoryId() {
        return categoryRepository.findByDefaultCategoryIsTrue()
                .map(Category::getId)
                .orElseThrow(() -> new BusinessException(
                        "This mart has no default category, so a category has to be named"));
    }

    // ── Working out what was meant ────────────────────────────────────────

    /**
     * The selling lines, whether they arrived as a list or as a bare price.
     */
    private List<ProductUnitLineRequest> sellLines(ProductCreateRequest request) {
        boolean shorthand = request.getSellingPrice() != null;
        boolean listed = request.getSellIn() != null && !request.getSellIn().isEmpty();

        if (shorthand && listed) {
            throw new BusinessException(
                    "Send either 'sellingPrice', for a product sold one at a time, or 'sellIn' "
                            + "for a product sold in several units — not both");
        }
        if (listed) {
            return request.getSellIn();
        }
        if (!shorthand) {
            return List.of();
        }
        return List.of(new ProductUnitLineRequest(
                null, null, null, null,
                request.getSellingPrice(), request.getMrp(),
                request.getSku(), request.getBarcode(), null, Boolean.TRUE));
    }

    private List<ProductUnitLineRequest> buyLines(ProductCreateRequest request) {
        boolean shorthand = request.getPurchasePrice() != null;
        boolean listed = request.getBuyIn() != null && !request.getBuyIn().isEmpty();

        if (shorthand && listed) {
            throw new BusinessException(
                    "Send either 'purchasePrice', for a product bought one at a time, or 'buyIn' "
                            + "for a product bought in several units — not both");
        }
        if (listed) {
            return request.getBuyIn();
        }
        if (!shorthand) {
            return List.of();
        }
        return List.of(new ProductUnitLineRequest(
                null, null, null, null,
                request.getPurchasePrice(), null, null, null, null, Boolean.TRUE));
    }

    /**
     * What stock gets counted in.
     * <p>
     * Taken from how the product is sold, because that is the thing its author
     * actually knows: say it sells by the kilogram and weight is what is being
     * measured, so grams are what the ledger holds. A caller may still name the
     * unit outright, and now has to name a sensible one.
     */
    private Unit baseUnitFor(ProductCreateRequest request,
                             List<ProductUnitLineRequest> sellLines,
                             List<ProductUnitLineRequest> buyLines) {

        if (request.getBaseUnitId() != null) {
            return resolver.unit(request.getBaseUnitId());
        }
        return unitFactory.baseUnitFor(anchorMeasurement(sellLines, buyLines));
    }

    /**
     * The measurement type the product trades in. Selling wins over buying: a
     * sack is a container, and what is inside it is the question. A product
     * created with no units at all is counted in pieces, which is what all but
     * a handful of a mart's lines are.
     */
    private MeasurementType anchorMeasurement(List<ProductUnitLineRequest> sellLines,
                                              List<ProductUnitLineRequest> buyLines) {
        return anchorIn(sellLines)
                .or(() -> anchorIn(buyLines))
                .orElse(MeasurementType.COUNT);
    }

    private Optional<MeasurementType> anchorIn(List<ProductUnitLineRequest> lines) {
        return lines.stream()
                .filter(ProductUnitLineRequest::wantsDefault)
                .findFirst()
                .or(() -> lines.stream().findFirst())
                .map(unitFactory::resolve)
                .map(Unit::getMeasurementType);
    }

    // ── Writing the trading configuration ─────────────────────────────────

    private void applySellLines(Product product, List<ProductUnitLineRequest> lines) {
        int defaultAt = defaultIndex(lines);
        Set<Long> seen = new HashSet<>();
        Set<String> barcodes = new HashSet<>();
        Set<String> skus = new HashSet<>();

        for (int i = 0; i < lines.size(); i++) {
            ProductUnitLineRequest line = lines.get(i);
            Unit unit = unitFor(product, line);

            requireNotRepeated(seen, unit, "sold");
            requireDistinctLabel(barcodes, line.barcode(), "barcode");
            requireDistinctLabel(skus, line.sku(), "SKU");
            validation.requireSameMeasurement(product.getBaseUnit(), unit);
            validation.requireBarcodeAvailable(line.barcode(), null);
            validation.requireSkuAvailable(line.sku(), null);
            grants.grantIfAbsent(product.getCategory(), unit, UnitUsage.SELLING);

            if (line.price() == null) {
                throw new BusinessException("A selling price is required for '" + unit.getName()
                        + "' — a product with no price cannot be rung up");
            }

            ProductSellingUnit sellingUnit = sellingUnitRepository.save(ProductSellingUnit.builder()
                    .product(product)
                    .unit(unit)
                    .packQuantity(unitFactory.packQuantity(product.getBaseUnit(), unit, line))
                    .sellingPrice(line.price())
                    .mrp(line.mrp())
                    .sku(line.sku())
                    .barcode(line.barcode())
                    .isDefault(i == defaultAt)
                    .active(true)
                    .build());
            product.addSellingUnit(sellingUnit);
        }
    }

    private void applyBuyLines(Product product, List<ProductUnitLineRequest> lines) {
        int defaultAt = defaultIndex(lines);
        Set<Long> seen = new HashSet<>();

        for (int i = 0; i < lines.size(); i++) {
            ProductUnitLineRequest line = lines.get(i);
            Unit unit = unitFor(product, line);

            requireNotRepeated(seen, unit, "bought");
            validation.requireSameMeasurement(product.getBaseUnit(), unit);
            grants.grantIfAbsent(product.getCategory(), unit, UnitUsage.PURCHASE);

            ProductPurchaseUnit purchaseUnit = purchaseUnitRepository.save(ProductPurchaseUnit.builder()
                    .product(product)
                    .unit(unit)
                    .packQuantity(unitFactory.packQuantity(product.getBaseUnit(), unit, line))
                    .purchasePrice(line.price())
                    .isDefault(i == defaultAt)
                    .active(true)
                    .build());
            product.addPurchaseUnit(purchaseUnit);

            openVatRate(purchaseUnit, new ProductVatRequest(
                    line.vatRate() == null ? taxProperties.vatRate() : line.vatRate()));
        }
    }

    /** A line naming no unit means the base unit — that is what makes the shorthand work. */
    private Unit unitFor(Product product, ProductUnitLineRequest line) {
        return line.namesUnit() ? unitFactory.resolve(line) : product.getBaseUnit();
    }

    /**
     * Exactly one default per side. An unclaimed default falls to the first
     * line, so the till always has something to reach for; a partial unique
     * index backs this up if two ever slip through.
     */
    private int defaultIndex(List<ProductUnitLineRequest> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).wantsDefault()) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Caught here rather than left to the unique index, because the index would
     * surface as a constraint violation naming a column, and the mistake is
     * almost always two lines of one request that meant different pack sizes.
     */
    private void requireNotRepeated(Set<Long> seen, Unit unit, String side) {
        if (!seen.add(unit.getId())) {
            throw new BusinessException(
                    "'" + unit.getName() + "' is listed twice as a way this product is " + side);
        }
    }

    /**
     * The validation counterparts check the table, which cannot see two lines
     * of the request that have not been written yet. A barcode scanned at the
     * till has to resolve to one row, so both halves of the check matter.
     */
    private void requireDistinctLabel(Set<String> seen, String value, String label) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!seen.add(value.trim().toLowerCase())) {
            throw new BusinessException("The same " + label + " ('" + value.trim()
                    + "') is on two of this product's selling units");
        }
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

    // ── How the product is bought ─────────────────────────────────────────

    @Override
    public ProductPurchaseUnitDetailResponse addPurchaseUnit(
            Long productId, ProductPurchaseUnitCreateRequest request) {

        Product product = resolver.product(productId);
        Unit unit = resolver.unit(request.getUnitId());

        grants.grantIfAbsent(product.getCategory(), unit, UnitUsage.PURCHASE);
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
        ProductPurchaseVat vat = vatRepository.save(ProductPurchaseVat.builder()
                .productPurchaseUnit(purchaseUnit)
                .rate(request.rate())
                .build());
        purchaseUnit.addVatRate(vat);
        log.info("VAT on purchase unit {} set to {}%", purchaseUnit.getId(), request.rate());
    }

    // ── How the product is sold ───────────────────────────────────────────

    @Override
    public ProductSellingUnitResponse addSellingUnit(Long productId, ProductSellingUnitCreateRequest request) {
        Product product = resolver.product(productId);
        Unit unit = resolver.unit(request.getUnitId());

        grants.grantIfAbsent(product.getCategory(), unit, UnitUsage.SELLING);
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
