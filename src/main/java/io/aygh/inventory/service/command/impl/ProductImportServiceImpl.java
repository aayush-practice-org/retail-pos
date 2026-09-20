package io.aygh.inventory.service.command.impl;

import io.aygh.exception.BusinessException;
import io.aygh.inventory.dto.request.PackSizeRequest;
import io.aygh.inventory.dto.request.ProductCreateRequest;
import io.aygh.inventory.dto.request.ProductUnitLineRequest;
import io.aygh.inventory.dto.response.ProductImportResponse;
import io.aygh.inventory.dto.response.ProductImportResponse.RowError;
import io.aygh.inventory.entity.Category;
import io.aygh.inventory.helper.CsvTable;
import io.aygh.inventory.helper.ProductUnitFactory;
import io.aygh.inventory.repository.CategoryRepository;
import io.aygh.inventory.repository.ProductRepository;
import io.aygh.inventory.repository.ProductSellingUnitRepository;
import io.aygh.inventory.service.command.ProductCommandService;
import io.aygh.inventory.service.command.ProductImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Bulk product entry, because no per-product screen gets a real mart open.
 * <p>
 * A shop with 800 lines is not going to type them into a form, and it almost
 * always already has them somewhere — a wholesaler's price list, last year's
 * stocktake, the spreadsheet the previous system exported. This is the path
 * from that file to a working catalogue.
 * <p>
 * It creates nothing itself: every row becomes a {@link ProductCreateRequest}
 * and goes through {@link ProductCommandService#create}, so an imported product
 * and a hand-entered one are subject to exactly the same rules. The work here
 * is the two things a single-product endpoint never has to do — read what
 * someone else's spreadsheet meant, and report on 600 rows at once.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImportServiceImpl implements ProductImportService {

    private static final String[] NAME = {"name", "productname", "product", "itemname"};
    private static final String[] CATEGORY = {"category", "categoryname", "aisle", "group"};
    private static final String[] SELL_PRICE = {"sellprice", "sellingprice", "price", "rate"};
    private static final String[] SELL_UNIT = {"sellunit", "sellin", "sellingunit", "unit"};
    private static final String[] BUY_PRICE = {"buyprice", "purchaseprice", "cost", "costprice"};
    private static final String[] BUY_UNIT = {"buyunit", "buyin", "purchaseunit"};
    private static final String[] PACK_QTY = {"packqty", "packsize", "packquantity", "contains"};
    private static final String[] PACK_UNIT = {"packunit", "containsunit", "packsizeunit"};
    private static final String[] BARCODE = {"barcode", "ean", "upc"};
    private static final String[] SKU = {"sku"};
    private static final String[] BRAND = {"brand"};
    private static final String[] PRODUCT_CODE = {"productcode", "itemcode", "code"};
    private static final String[] MRP = {"mrp", "listprice", "printedprice"};
    private static final String[] VAT = {"vatrate", "vat", "tax", "taxrate"};
    private static final String[] DESCRIPTION = {"description", "desc", "details"};

    /** Separates the two halves of a composite key, so "a|b" and "ab" differ. */
    private static final String KEY_SEPARATOR = "::";

    private final ProductCommandService productCommandService;
    private final ProductUnitFactory unitFactory;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductSellingUnitRepository sellingUnitRepository;

    @Override
    @Transactional
    public ProductImportResponse importFrom(MultipartFile file, boolean dryRun) {
        CsvTable table = read(file);

        if (!table.has(NAME)) {
            throw new BusinessException("The file needs a 'name' column");
        }
        if (!table.has(SELL_PRICE)) {
            throw new BusinessException("The file needs a 'sell_price' column");
        }
        if (table.size() == 0) {
            throw new BusinessException("The file has a header but no rows");
        }

        List<RowError> errors = new ArrayList<>();
        List<ProductCreateRequest> requests = new ArrayList<>();

        // ── Check the whole file before writing any of it ─────────────────
        //
        // Deliberately not "write each row and roll back on the first failure":
        // that reports one error per upload, and a spreadsheet with a dozen
        // small problems would take a dozen round trips to fix. Everything
        // knowable without writing is checked here, so the usual bad import
        // comes back complete and gets fixed in one pass.
        Seen seen = new Seen();
        Map<String, Category> categories = new HashMap<>();

        for (int i = 0; i < table.size(); i++) {
            CsvTable.Row row = table.row(i);
            try {
                requests.add(toRequest(row, categories, seen));
            } catch (BusinessException e) {
                errors.add(new RowError(row.lineNumber(), row.get(NAME), e.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            log.info("Rejected an import of {} row(s): {} problem(s)", table.size(), errors.size());
            return ProductImportResponse.rejected(table.size(), dryRun, errors);
        }
        if (dryRun) {
            return ProductImportResponse.checked(table.size());
        }

        requests.forEach(productCommandService::create);

        log.info("Imported {} product(s)", requests.size());
        return ProductImportResponse.imported(table.size());
    }

    // ── One row ───────────────────────────────────────────────────────────

    private ProductCreateRequest toRequest(CsvTable.Row row, Map<String, Category> categories, Seen seen) {

        String name = require(row.get(NAME), "A name is required");
        Category category = category(row.get(CATEGORY), categories);

        ProductCreateRequest request = new ProductCreateRequest();
        request.setName(name);
        request.setCategoryId(category.getId());
        request.setBrand(row.get(BRAND));
        request.setDescription(row.get(DESCRIPTION));
        request.setProductCode(row.get(PRODUCT_CODE));

        String barcode = row.get(BARCODE);
        String sku = row.get(SKU);
        String sellUnit = row.get(SELL_UNIT);

        request.setSellIn(List.of(new ProductUnitLineRequest(
                sellUnit, null, null, null,
                number(row.get(SELL_PRICE), "sell price"),
                number(row.get(MRP), "MRP"),
                sku, barcode, null, Boolean.TRUE)));

        BigDecimal buyPrice = number(row.get(BUY_PRICE), "buy price");
        String buyUnit = row.get(BUY_UNIT);
        if (buyPrice != null || buyUnit != null) {
            request.setBuyIn(List.of(new ProductUnitLineRequest(
                    buyUnit != null ? buyUnit : sellUnit, null,
                    packSize(row), null, buyPrice, null, null, null,
                    number(row.get(VAT), "VAT rate"), Boolean.TRUE)));
        }

        // Cheap to check here, and the alternative is a unique-index violation
        // partway through the write that takes the whole import down with it.
        checkUnits(row, sellUnit, buyUnit);
        seen.claimName(name, category, row);
        seen.claimBarcode(barcode);
        seen.claimSku(sku);
        claimProductCode(request.getProductCode());
        return request;
    }

    /**
     * Resolves the unit names now rather than letting {@code create} discover
     * them. A typo in a unit name is the single most common thing wrong with
     * one of these files, and it should be reported beside every other problem
     * rather than being the one that aborts the run.
     */
    private void checkUnits(CsvTable.Row row, String sellUnit, String buyUnit) {
        if (sellUnit != null) {
            unitFactory.resolve(null, sellUnit);
        }
        if (buyUnit != null) {
            unitFactory.resolve(null, buyUnit);
        }
        String packUnit = row.get(PACK_UNIT);
        if (packUnit != null) {
            unitFactory.resolve(null, packUnit);
        }
    }

    private PackSizeRequest packSize(CsvTable.Row row) {
        BigDecimal qty = number(row.get(PACK_QTY), "pack size");
        if (qty == null) {
            return null;
        }
        return new PackSizeRequest(qty, row.get(PACK_UNIT), null);
    }

    /**
     * Cached per import: a 600-row file is usually a dozen categories, and
     * looking each one up per row is 600 queries to learn twelve things.
     */
    private Category category(String name, Map<String, Category> cache) {
        if (name == null) {
            return cache.computeIfAbsent("", key -> categoryRepository.findByDefaultCategoryIsTrue()
                    .orElseThrow(() -> new BusinessException(
                            "No category given and this mart has no default category")));
        }
        return cache.computeIfAbsent(name.toLowerCase(Locale.ROOT),
                key -> categoryRepository.findByNameIgnoreCase(name)
                        .orElseThrow(() -> new BusinessException(
                                "No category called '" + name + "' — create it first, "
                                        + "or leave the column blank to use the default")));
    }

    private void claimProductCode(String productCode) {
        if (productCode != null && productRepository.existsByProductCodeIgnoreCase(productCode)) {
            throw new BusinessException("Product code '" + productCode + "' is already in use");
        }
    }

    // ── Reading ───────────────────────────────────────────────────────────

    private CsvTable read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("No file was uploaded");
        }
        try {
            return CsvTable.parse(new String(file.getBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new BusinessException("The uploaded file could not be read: " + e.getMessage());
        }
    }

    private static String require(String value, String message) {
        if (value == null) {
            throw new BusinessException(message);
        }
        return value;
    }

    /**
     * Tolerant of the thousands separators and stray spaces a spreadsheet
     * leaves behind, and specific about what it choked on when it cannot cope.
     */
    private static BigDecimal number(String value, String what) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replace(",", "").replace(" ", "").trim();
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new BusinessException("'" + value + "' is not a number, for " + what);
        }
    }

    /**
     * The duplicates a per-row check cannot see: two rows of one file claiming
     * the same barcode, plus anything the catalogue already holds.
     */
    private final class Seen {

        private final Set<String> names = new HashSet<>();
        private final Set<String> barcodes = new HashSet<>();
        private final Set<String> skus = new HashSet<>();

        void claimName(String name, Category category, CsvTable.Row row) {
            String key = category.getId() + KEY_SEPARATOR + name.toLowerCase(Locale.ROOT);
            if (!names.add(key)) {
                throw new BusinessException("'" + name + "' appears more than once in '"
                        + category.getName() + "', and this is line " + row.lineNumber());
            }
            if (productRepository.existsByNameIgnoreCaseAndCategoryId(name, category.getId())) {
                throw new BusinessException("'" + name + "' is already in '" + category.getName() + "'");
            }
        }

        void claimBarcode(String barcode) {
            if (barcode == null) {
                return;
            }
            if (!barcodes.add(barcode)) {
                throw new BusinessException("Barcode '" + barcode + "' appears twice in the file");
            }
            if (sellingUnitRepository.existsByBarcode(barcode)) {
                throw new BusinessException("Barcode '" + barcode + "' is already assigned");
            }
        }

        void claimSku(String sku) {
            if (sku == null) {
                return;
            }
            if (!skus.add(sku.toLowerCase(Locale.ROOT))) {
                throw new BusinessException("SKU '" + sku + "' appears twice in the file");
            }
            if (sellingUnitRepository.existsBySkuIgnoreCase(sku)) {
                throw new BusinessException("SKU '" + sku + "' is already assigned");
            }
        }
    }
}
