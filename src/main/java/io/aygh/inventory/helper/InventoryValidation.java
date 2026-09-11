package io.aygh.inventory.helper;

import io.aygh.exception.BusinessException;
import io.aygh.inventory.entity.Category;
import io.aygh.inventory.entity.MeasurementType;
import io.aygh.inventory.entity.Unit;
import io.aygh.inventory.entity.UnitUsage;
import io.aygh.inventory.repository.CategoryRepository;
import io.aygh.inventory.repository.CategoryUnitRepository;
import io.aygh.inventory.repository.ProductPurchaseUnitRepository;
import io.aygh.inventory.repository.ProductRepository;
import io.aygh.inventory.repository.ProductSellingUnitRepository;
import io.aygh.inventory.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * The rules the entities cannot state for themselves.
 * <p>
 * Uniqueness is checked here <em>and</em> backed by a partial unique index in
 * the migration. The check is what produces a readable message; the index is
 * what actually holds under two concurrent requests.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryValidation {

    private final UnitRepository unitRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryUnitRepository categoryUnitRepository;
    private final ProductRepository productRepository;
    private final ProductPurchaseUnitRepository purchaseUnitRepository;
    private final ProductSellingUnitRepository sellingUnitRepository;

    // ── Units ─────────────────────────────────────────────────────────────

    public void requireUnitNameAvailable(String name, Long excludingId) {
        boolean taken = excludingId == null
                ? unitRepository.existsByNameIgnoreCase(name)
                : unitRepository.existsByNameIgnoreCaseAndIdNot(name, excludingId);
        if (taken) {
            throw new BusinessException("A unit named '" + name + "' already exists");
        }
    }

    public void requireUnitSymbolAvailable(String symbol, Long excludingId) {
        boolean taken = excludingId == null
                ? unitRepository.existsBySymbolIgnoreCase(symbol)
                : unitRepository.existsBySymbolIgnoreCaseAndIdNot(symbol, excludingId);
        if (taken) {
            throw new BusinessException("A unit with symbol '" + symbol + "' already exists");
        }
    }

    /**
     * Seeded units are shared vocabulary the rest of the catalogue is measured
     * against. Editing "Kilogram" to mean something else would silently restate
     * every quantity already recorded through it.
     */
    public void rejectSystemUnitChange(Unit unit, String action) {
        if (unit.isSystemDefined()) {
            throw new BusinessException("'" + unit.getName() + "' is a built-in unit and cannot be " + action);
        }
    }

    /**
     * A unit still authorised by a category, or configured on a product, is in use.
     */
    public void requireUnitUnused(Unit unit) {
        if (categoryUnitRepository.existsByUnitId(unit.getId())) {
            throw new BusinessException(
                    "'" + unit.getName() + "' is still permitted by at least one category");
        }
    }

    // ── Categories ────────────────────────────────────────────────────────

    public void requireCategoryNameAvailable(String name, Long excludingId) {
        boolean taken = excludingId == null
                ? categoryRepository.existsByNameIgnoreCase(name)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name, excludingId);
        if (taken) {
            throw new BusinessException("A category named '" + name + "' already exists");
        }
    }

    public void requireCategoryEmpty(Long categoryId) {
        long products = productRepository.countByCategoryId(categoryId);
        if (products > 0) {
            throw new BusinessException(
                    "This category still holds " + products + " product(s); move or retire them first");
        }
    }

    public void requireUnitNotAlreadyPermitted(Long categoryId, Long unitId, UnitUsage usage) {
        if (categoryUnitRepository.existsByCategoryIdAndUnitIdAndUsage(categoryId, unitId, usage)) {
            throw new BusinessException("That unit is already permitted for " + usage);
        }
    }

    // ── Products ──────────────────────────────────────────────────────────

    public void requireProductCodeAvailable(String productCode, Long excludingId) {
        if (productCode == null || productCode.isBlank()) {
            return;
        }
        boolean taken = excludingId == null
                ? productRepository.existsByProductCodeIgnoreCase(productCode)
                : productRepository.existsByProductCodeIgnoreCaseAndIdNot(productCode, excludingId);
        if (taken) {
            throw new BusinessException("Product code '" + productCode + "' is already in use");
        }
    }

    /**
     * Names only have to be unique within a category, so two aisles may both stock "Basmati".
     */
    public void requireProductNameAvailable(String name, Long categoryId, Long excludingId) {
        boolean taken = excludingId == null
                ? productRepository.existsByNameIgnoreCaseAndCategoryId(name, categoryId)
                : productRepository.existsByNameIgnoreCaseAndCategoryIdAndIdNot(name, categoryId, excludingId);
        if (taken) {
            throw new BusinessException("A product named '" + name + "' already exists in this category");
        }
    }

    /**
     * Stock is held in the base unit, so it has to be one with a fixed size. A
     * Sack means a different amount for every product; counting inventory in
     * them makes two products' stock figures incomparable.
     */
    public void requireUsableAsBaseUnit(Unit unit) {
        if (!unit.isReferenceUnit()) {
            log.debug("Base unit '{}' is not the reference unit of {}", unit.getName(), unit.getMeasurementType());
        }
    }

    // ── Trading configuration ─────────────────────────────────────────────

    /**
     * The check that makes {@code CategoryUnit} mean something. Without it the
     * category's unit policy is decoration: a product could be configured to
     * sell in a unit its category never authorised.
     */
    public void requirePermittedByCategory(Category category, Unit unit, UnitUsage usage) {
        boolean permitted = categoryUnitRepository
                .existsByCategoryIdAndUnitIdAndUsage(category.getId(), unit.getId(), usage);
        if (!permitted) {
            throw new BusinessException("Category '" + category.getName() + "' does not permit '"
                    + unit.getName() + "' for " + usage
                    + " — authorise it on the category first");
        }
    }

    /**
     * Both units must measure the same thing as the product's base unit, or the
     * pack quantity converts between incompatible dimensions.
     */
    public void requireSameMeasurement(Unit baseUnit, Unit unit) {
        MeasurementType base = baseUnit.getMeasurementType();
        if (base != unit.getMeasurementType()) {
            throw new BusinessException("'" + unit.getName() + "' measures " + unit.getMeasurementType()
                    + ", but this product is counted in " + base);
        }
    }

    public void requirePurchaseUnitNotConfigured(Long productId, Long unitId, Long excludingId) {
        boolean taken = excludingId == null
                ? purchaseUnitRepository.existsByProductIdAndUnitId(productId, unitId)
                : purchaseUnitRepository.existsByProductIdAndUnitIdAndIdNot(productId, unitId, excludingId);
        if (taken) {
            throw new BusinessException("This product already has a purchase configuration for that unit");
        }
    }

    public void requireSellingUnitNotConfigured(Long productId, Long unitId, Long excludingId) {
        boolean taken = excludingId == null
                ? sellingUnitRepository.existsByProductIdAndUnitId(productId, unitId)
                : sellingUnitRepository.existsByProductIdAndUnitIdAndIdNot(productId, unitId, excludingId);
        if (taken) {
            throw new BusinessException("This product already has a selling configuration for that unit");
        }
    }

    /**
     * Barcodes are scanned at the till, so one must resolve to exactly one row.
     */
    public void requireBarcodeAvailable(String barcode, Long excludingId) {
        if (barcode == null || barcode.isBlank()) {
            return;
        }
        boolean taken = excludingId == null
                ? sellingUnitRepository.existsByBarcode(barcode)
                : sellingUnitRepository.existsByBarcodeAndIdNot(barcode, excludingId);
        if (taken) {
            throw new BusinessException("Barcode '" + barcode + "' is already assigned");
        }
    }

    public void requireSkuAvailable(String sku, Long excludingId) {
        if (sku == null || sku.isBlank()) {
            return;
        }
        boolean taken = excludingId == null
                ? sellingUnitRepository.existsBySkuIgnoreCase(sku)
                : sellingUnitRepository.existsBySkuIgnoreCaseAndIdNot(sku, excludingId);
        if (taken) {
            throw new BusinessException("SKU '" + sku + "' is already assigned");
        }
    }
}
