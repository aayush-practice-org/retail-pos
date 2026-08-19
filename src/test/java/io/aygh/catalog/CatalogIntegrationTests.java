package io.aygh.catalog;

import io.aygh.catalog.dto.request.CategoryRequest;
import io.aygh.catalog.dto.request.ProductRequest;
import io.aygh.catalog.dto.request.ProductVariantRequest;
import io.aygh.catalog.dto.response.CategoryResponse;
import io.aygh.catalog.dto.response.ProductResponse;
import io.aygh.catalog.dto.response.ProductVariantResponse;
import io.aygh.catalog.service.command.CategoryCommandService;
import io.aygh.catalog.service.command.ProductCommandService;
import io.aygh.catalog.service.command.ProductVariantCommandService;
import io.aygh.catalog.service.query.CategoryQueryService;
import io.aygh.catalog.service.query.ProductQueryService;
import io.aygh.catalog.service.query.ProductVariantQueryService;
import io.aygh.exception.BusinessException;
import io.aygh.shared.response.PagedResponse;
import io.aygh.unit.entity.MeasurementType;
import io.aygh.unit.entity.SystemUnit;
import io.aygh.unit.entity.UnitSource;
import io.aygh.unit.repository.SystemUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CatalogIntegrationTests {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private CategoryCommandService categoryCommandService;
    @Autowired
    private CategoryQueryService categoryQueryService;
    @Autowired
    private ProductCommandService productCommandService;
    @Autowired
    private ProductQueryService productQueryService;
    @Autowired
    private ProductVariantCommandService variantCommandService;
    @Autowired
    private ProductVariantQueryService variantQueryService;
    @Autowired
    private SystemUnitRepository systemUnitRepository;

    private UUID gram;
    private UUID kilogram;
    private UUID piece;

    @BeforeEach
    void seedSystemUnits() {
        gram = systemUnit("Gram", "g", MeasurementType.WEIGHT, BigDecimal.ONE, true);
        kilogram = systemUnit("Kilogram", "kg", MeasurementType.WEIGHT, new BigDecimal("1000"), false);
        piece = systemUnit("Piece", "pc", MeasurementType.COUNT, BigDecimal.ONE, true);
    }

    @Test
    void subcategoriesNestUnderTheirParent() {
        Long grocery = category("Grocery", null);
        Long beverages = category("Beverages", grocery);

        CategoryResponse tree = categoryQuery(grocery);

        assertThat(tree.children()).extracting(CategoryResponse::id).containsExactly(beverages);
        assertThat(tree.children().getFirst().parentId()).isEqualTo(grocery);
    }

    @Test
    void productsAreFoundThroughAParentCategory() {
        Long grocery = category("Grocery", null);
        Long softDrinks = category("Soft Drinks", category("Beverages", grocery));

        Long product = product("Coca-Cola", softDrinks, gram);

        assertThat(searchIds(grocery, true)).contains(product);
        assertThat(searchIds(grocery, false)).doesNotContain(product);
        assertThat(searchIds(softDrinks, false)).contains(product);
    }

    @Test
    void aCategoryStillHoldingProductsCannotBeDeleted() {
        Long snacks = category("Snacks", null);
        product("Lays", snacks, piece);

        assertThatThrownBy(() -> categoryCommandService.delete(snacks))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("product(s) are still assigned");
    }

    @Test
    void theFirstVariantOfAProductBecomesItsDefault() {
        Long product = product("Basmati Rice", category("Grains", null), gram);

        ProductVariantResponse first = variant(product, "1 kg pouch", new BigDecimal("1000"), null, false);

        assertThat(first.isDefault()).isTrue();
    }

    @Test
    void packSizeEnteredInAnotherUnitIsConvertedToTheProductsBaseUnit() {
        Long product = product("Sugar", category("Grains", null), gram);

        ProductVariantResponse entered = variant(product, "1 kg pack", BigDecimal.ONE, kilogram, false);

        assertThat(entered.packSize()).isEqualByComparingTo("1000");
        assertThat(entered.baseUnitSymbol()).isEqualTo("g");
        assertThat(entered.originalQuantity()).isEqualByComparingTo("1");
        assertThat(entered.originalUnitId()).isEqualTo(kilogram);
        assertThat(entered.originalUnitSource()).isEqualTo(UnitSource.SYSTEM);
    }

    @Test
    void aPackUnitMeasuringSomethingElseIsRejected() {
        Long product = product("Wheat Flour", category("Grains", null), gram);

        assertThatThrownBy(() -> variant(product, "Sack", BigDecimal.ONE, piece, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("measure different things");
    }

    @Test
    void promotingAVariantDemotesThePreviousDefault() {
        Long product = product("Cooking Oil", category("Oils", null), gram);

        ProductVariantResponse small = variant(product, "500 g", new BigDecimal("500"), null, false);
        ProductVariantResponse large = variant(product, "1 kg", BigDecimal.ONE, kilogram, true);

        assertThat(large.isDefault()).isTrue();
        assertThat(defaults(product)).containsExactly(large.id());

        variantCommandService.setDefault(product, small.id());

        assertThat(defaults(product)).containsExactly(small.id());
    }

    @Test
    void theDefaultVariantCannotSimplyBeUnset() {
        Long product = product("Tea Leaves", category("Beverages", null), gram);

        ProductVariantResponse original = variant(product, "250 g", new BigDecimal("250"), null, false);
        variant(product, "500 g", new BigDecimal("500"), null, false);

        assertThatThrownBy(() -> variantCommandService.update(product, original.id(),
                request("250 g", new BigDecimal("250"), null, false)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mark another variant as default");
    }

    @Test
    void deletingTheDefaultVariantPromotesTheNextOne() {
        Long product = product("Milk Powder", category("Dairy", null), gram);

        ProductVariantResponse first = variant(product, "200 g", new BigDecimal("200"), null, false);
        ProductVariantResponse second = variant(product, "400 g", new BigDecimal("400"), null, false);

        variantCommandService.delete(product, first.id());

        assertThat(defaults(product)).containsExactly(second.id());
        assertThat(variantQueryService.findByProduct(product)).hasSize(1);
    }

    @Test
    void variantNamesAreIsolatedToTheirOwnProduct() {
        Long category = category("Soft Drinks", null);
        Long coke = product("Coca-Cola", category, gram);
        Long fanta = product("Fanta", category, gram);

        variant(coke, "500 ml", new BigDecimal("500"), null, false);

        assertThatThrownBy(() -> variant(coke, "500 ml", new BigDecimal("500"), null, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already has a variant named");

        // the same variant name on a different product is a different thing entirely
        assertThat(variant(fanta, "500 ml", new BigDecimal("500"), null, false).id()).isNotNull();
    }

    @Test
    void sellingPriceCannotExceedTheMrp() {
        Long product = product("Biscuits", category("Snacks", null), piece);

        assertThatThrownBy(() -> variantCommandService.create(product, new ProductVariantRequest(
                "Family pack", null, null, BigDecimal.ONE, null, null,
                new BigDecimal("50.00"), new BigDecimal("60.00"), false, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be higher than the MRP");
    }

    @Test
    void theBaseUnitIsLockedOnceVariantsExist() {
        Long category = category("Grains", null);
        Long product = product("Lentils", category, gram);
        variant(product, "500 g", new BigDecimal("500"), null, false);

        assertThatThrownBy(() -> productCommandService.update(product, new ProductRequest(
                "Lentils", null, null, category, piece, null, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot change the base unit");
    }

    // ── fixtures ──────────────────────────────────────────────────────────

    private UUID systemUnit(String name, String symbol, MeasurementType type, BigDecimal factor, boolean base) {
        return systemUnitRepository.findByName(name)
                .orElseGet(() -> systemUnitRepository.save(SystemUnit.builder()
                        .name(name)
                        .symbol(symbol)
                        .measurementType(type)
                        .conversionFactor(factor)
                        .isBaseUnit(base)
                        .build()))
                .getId();
    }

    private Long category(String name, Long parentId) {
        return categoryCommandService.create(
                new CategoryRequest(unique(name), null, null, parentId)).id();
    }

    private CategoryResponse categoryQuery(Long id) {
        return categoryQueryService.findById(id);
    }

    private Long product(String name, Long categoryId, UUID baseUnitId) {
        return productCommandService.create(new ProductRequest(
                unique(name), null, null, categoryId, baseUnitId, null, true)).id();
    }

    private ProductVariantResponse variant(Long productId, String name, BigDecimal packSize,
                                           UUID packUnitId, boolean isDefault) {
        return variantCommandService.create(productId, request(name, packSize, packUnitId, isDefault));
    }

    private ProductVariantRequest request(String name, BigDecimal packSize, UUID packUnitId, boolean isDefault) {
        return new ProductVariantRequest(
                name, null, null, packSize, packUnitId,
                packUnitId == null ? null : UnitSource.SYSTEM,
                null, new BigDecimal("100.00"), isDefault, true);
    }

    private List<Long> defaults(Long productId) {
        return variantQueryService.findByProduct(productId).stream()
                .filter(ProductVariantResponse::isDefault)
                .map(ProductVariantResponse::id)
                .toList();
    }

    private List<Long> searchIds(Long categoryId, boolean includeSubCategories) {
        PagedResponse<ProductResponse> page = productQueryService.findAll(
                null, categoryId, null, includeSubCategories, PageRequest.of(0, 50));

        return page.getContent().stream().map(ProductResponse::id).toList();
    }

    private static String unique(String prefix) {
        return prefix + " " + SEQ.incrementAndGet();
    }
}
