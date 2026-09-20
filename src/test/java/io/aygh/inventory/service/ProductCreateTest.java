package io.aygh.inventory.service;

import io.aygh.config.properties.TaxProperties;
import io.aygh.exception.BusinessException;
import io.aygh.inventory.dto.request.PackSizeRequest;
import io.aygh.inventory.dto.request.ProductCreateRequest;
import io.aygh.inventory.dto.request.ProductUnitLineRequest;
import io.aygh.inventory.dto.request.QuickAddRequest;
import io.aygh.inventory.dto.response.ProductDetailResponse;
import io.aygh.inventory.dto.response.ProductSellingUnitResponse;
import io.aygh.inventory.entity.Category;
import io.aygh.inventory.entity.MeasurementType;
import io.aygh.inventory.entity.Product;
import io.aygh.inventory.entity.ProductPurchaseUnit;
import io.aygh.inventory.entity.ProductPurchaseVat;
import io.aygh.inventory.entity.ProductSellingUnit;
import io.aygh.inventory.entity.Unit;
import io.aygh.inventory.entity.UnitUsage;
import io.aygh.inventory.helper.CategoryUnitGrants;
import io.aygh.inventory.helper.InventoryResolver;
import io.aygh.inventory.helper.InventoryValidation;
import io.aygh.inventory.helper.ProductUnitFactory;
import io.aygh.inventory.mapper.ProductMapper;
import io.aygh.inventory.mapper.ProductUnitMapper;
import io.aygh.inventory.repository.CategoryRepository;
import io.aygh.inventory.repository.ProductPurchaseUnitRepository;
import io.aygh.inventory.repository.ProductPurchaseVatRepository;
import io.aygh.inventory.repository.ProductRepository;
import io.aygh.inventory.repository.ProductSellingUnitRepository;
import io.aygh.inventory.service.command.impl.ProductCommandServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * What a create request turns into. The point of the composite endpoint is that
 * most of a product's configuration is never typed, so these are mostly
 * assertions about what the caller left out.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductCreateTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductPurchaseUnitRepository purchaseUnitRepository;
    @Mock private ProductSellingUnitRepository sellingUnitRepository;
    @Mock private ProductPurchaseVatRepository vatRepository;
    @Mock private InventoryResolver resolver;
    @Mock private InventoryValidation validation;
    @Mock private ProductUnitFactory unitFactory;
    @Mock private CategoryUnitGrants grants;
    @Mock private ProductMapper productMapper;
    @Mock private ProductUnitMapper productUnitMapper;

    private ProductCommandServiceImpl service;

    private Category grocery;
    private Unit gram;
    private Unit kilogram;
    private Unit sack;
    private Unit piece;

    @BeforeEach
    void setUp() {
        service = new ProductCommandServiceImpl(
                productRepository, categoryRepository,
                purchaseUnitRepository, sellingUnitRepository, vatRepository,
                resolver, validation, unitFactory, grants, new TaxProperties(new BigDecimal("13")),
                productMapper, productUnitMapper);

        grocery = new Category();
        grocery.setId(7L);
        grocery.setName("Grocery");

        gram = unit(1L, "Gram", "g", MeasurementType.WEIGHT, true);
        kilogram = unit(2L, "Kilogram", "kg", MeasurementType.WEIGHT, false);
        sack = unit(3L, "Sack", "sack", MeasurementType.WEIGHT, false);
        piece = unit(4L, "Piece", "pc", MeasurementType.COUNT, true);

        when(resolver.category(7L)).thenReturn(grocery);
        when(unitFactory.baseUnitFor(MeasurementType.COUNT)).thenReturn(piece);
        when(unitFactory.baseUnitFor(MeasurementType.WEIGHT)).thenReturn(gram);
        when(unitFactory.packQuantity(any(), any(), any())).thenReturn(BigDecimal.ONE);

        when(productMapper.toEntity(any(ProductCreateRequest.class))).thenAnswer(call -> {
            ProductCreateRequest request = call.getArgument(0);
            Product product = new Product();
            product.setName(request.getName());
            return product;
        });
        when(productRepository.save(any(Product.class))).thenAnswer(call -> {
            Product product = call.getArgument(0);
            product.setId(99L);
            return product;
        });
        when(sellingUnitRepository.save(any(ProductSellingUnit.class))).thenAnswer(call -> call.getArgument(0));
        when(purchaseUnitRepository.save(any(ProductPurchaseUnit.class))).thenAnswer(call -> call.getArgument(0));
        when(vatRepository.save(any(ProductPurchaseVat.class))).thenAnswer(call -> call.getArgument(0));
        when(resolver.productDetail(99L)).thenReturn(new Product());
        when(productMapper.toDetail(any(Product.class))).thenReturn(new ProductDetailResponse());
    }

    // ── The nine-in-ten case ──────────────────────────────────────────────

    @Test
    void aPackagedProductNeedsNoUnitsAtAll() {
        ProductCreateRequest request = base("Wai Wai Noodles");
        request.setPurchasePrice(new BigDecimal("20"));
        request.setSellingPrice(new BigDecimal("25"));
        request.setBarcode("8901234567890");

        service.create(request);

        ProductSellingUnit sold = captureSold();
        assertEquals(piece, sold.getUnit(), "stock falls back to pieces when nothing says otherwise");
        assertEquals(0, new BigDecimal("25").compareTo(sold.getSellingPrice()));
        assertEquals("8901234567890", sold.getBarcode());
        assertTrue(sold.isDefault(), "the only selling unit has to be the one the till reaches for");

        ProductPurchaseUnit bought = captureBought();
        assertEquals(piece, bought.getUnit());
        assertEquals(0, new BigDecimal("20").compareTo(bought.getPurchasePrice()));
        assertTrue(bought.isDefault());
    }

    @Test
    void theMartsVatRateAppliesWhenNoneIsGiven() {
        ProductCreateRequest request = base("Wai Wai Noodles");
        request.setPurchasePrice(new BigDecimal("20"));

        service.create(request);

        ArgumentCaptor<ProductPurchaseVat> captor = ArgumentCaptor.forClass(ProductPurchaseVat.class);
        verify(vatRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("13").compareTo(captor.getValue().getRate()));
    }

    @Test
    void anExemptProductCanSayZero() {
        ProductCreateRequest request = base("Rice");
        request.setBuyIn(List.of(new ProductUnitLineRequest(
                "kg", null, null, null, new BigDecimal("120"),
                null, null, null, BigDecimal.ZERO, null)));
        when(unitFactory.resolve(any(ProductUnitLineRequest.class))).thenReturn(kilogram);

        service.create(request);

        ArgumentCaptor<ProductPurchaseVat> captor = ArgumentCaptor.forClass(ProductPurchaseVat.class);
        verify(vatRepository).save(captor.capture());
        assertEquals(0, BigDecimal.ZERO.compareTo(captor.getValue().getRate()));
    }

    // ── Derivation ────────────────────────────────────────────────────────

    @Test
    void stockIsCountedInTheReferenceUnitOfWhateverItIsSoldIn() {
        ProductCreateRequest request = base("Basmati Rice");
        request.setSellIn(List.of(sellLine("kg", "145", false)));
        when(unitFactory.resolve(any(ProductUnitLineRequest.class))).thenReturn(kilogram);

        service.create(request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals(gram, captor.getValue().getBaseUnit(),
                "sold by the kilogram means weight, and weight is held in grams");
    }

    @Test
    void theCategoryLearnsTheUnitsRatherThanGatingThem() {
        ProductCreateRequest request = base("Wai Wai Noodles");
        request.setSellingPrice(new BigDecimal("25"));
        request.setPurchasePrice(new BigDecimal("20"));

        service.create(request);

        verify(grants).grantIfAbsent(grocery, piece, UnitUsage.SELLING);
        verify(grants).grantIfAbsent(grocery, piece, UnitUsage.PURCHASE);
    }

    @Test
    void anUnclaimedDefaultFallsToTheFirstLine() {
        ProductCreateRequest request = base("Basmati Rice");
        request.setSellIn(List.of(sellLine("kg", "145", false), sellLine("g", "0.15", false)));
        when(unitFactory.resolve(any(ProductUnitLineRequest.class))).thenReturn(kilogram, kilogram, gram);

        service.create(request);

        List<ProductSellingUnit> saved = captureAllSold();
        assertTrue(saved.get(0).isDefault());
        assertFalse(saved.get(1).isDefault());
    }

    @Test
    void aClaimedDefaultIsHonoured() {
        ProductCreateRequest request = base("Basmati Rice");
        request.setSellIn(List.of(sellLine("g", "0.15", false), sellLine("kg", "145", true)));
        when(unitFactory.resolve(any(ProductUnitLineRequest.class))).thenReturn(kilogram, gram, kilogram);

        service.create(request);

        List<ProductSellingUnit> saved = captureAllSold();
        assertFalse(saved.get(0).isDefault());
        assertTrue(saved.get(1).isDefault());
    }

    // ── Refusals ──────────────────────────────────────────────────────────

    @Test
    void shorthandAndTheFullFormTogetherAreRejectedRatherThanMerged() {
        ProductCreateRequest request = base("Basmati Rice");
        request.setSellingPrice(new BigDecimal("145"));
        request.setSellIn(List.of(sellLine("kg", "145", false)));

        BusinessException thrown = assertThrows(BusinessException.class, () -> service.create(request));
        assertTrue(thrown.getMessage().contains("not both"));
    }

    @Test
    void thesameUnitListedTwiceIsRejected() {
        ProductCreateRequest request = base("Basmati Rice");
        request.setSellIn(List.of(sellLine("kg", "145", false), sellLine("kg", "150", false)));
        when(unitFactory.resolve(any(ProductUnitLineRequest.class))).thenReturn(kilogram);

        BusinessException thrown = assertThrows(BusinessException.class, () -> service.create(request));
        assertTrue(thrown.getMessage().contains("twice"));
    }

    @Test
    void aSellingLineWithNoPriceIsRejected() {
        ProductCreateRequest request = base("Basmati Rice");
        request.setSellIn(List.of(new ProductUnitLineRequest(
                "kg", null, null, null, null, null, null, null, null, null)));
        when(unitFactory.resolve(any(ProductUnitLineRequest.class))).thenReturn(kilogram);

        BusinessException thrown = assertThrows(BusinessException.class, () -> service.create(request));
        assertTrue(thrown.getMessage().contains("selling price"));
    }

    @Test
    void anExplicitBaseUnitIsStillChecked() {
        ProductCreateRequest request = base("Basmati Rice");
        request.setBaseUnitId(3L);
        request.setSellingPrice(new BigDecimal("145"));
        when(resolver.unit(3L)).thenReturn(sack);
        doThrow(new BusinessException("Stock cannot be counted in 'Sack'"))
                .when(validation).requireUsableAsBaseUnit(sack);

        assertThrows(BusinessException.class, () -> service.create(request));
    }

    // ── A product bought in bulk and sold loose ───────────────────────────

    @Test
    void bulkInAndLooseOutConfiguresBothSidesAgainstOneBaseUnit() {
        ProductCreateRequest request = base("Basmati Rice");
        request.setSellIn(List.of(sellLine("kg", "145", true)));
        request.setBuyIn(List.of(new ProductUnitLineRequest(
                "Sack", null, new PackSizeRequest(new BigDecimal("50"), "kg", null),
                null, new BigDecimal("5800"), null, null, null, null, null)));

        when(unitFactory.resolve(any(ProductUnitLineRequest.class))).thenReturn(kilogram, kilogram, sack);
        when(unitFactory.packQuantity(eq(gram), eq(kilogram), any())).thenReturn(new BigDecimal("1000"));
        when(unitFactory.packQuantity(eq(gram), eq(sack), any())).thenReturn(new BigDecimal("50000"));

        service.create(request);

        assertEquals(0, new BigDecimal("1000").compareTo(captureSold().getPackQuantity()));
        ProductPurchaseUnit bought = captureBought();
        assertEquals(sack, bought.getUnit());
        assertEquals(0, new BigDecimal("50000").compareTo(bought.getPackQuantity()));
        verify(grants).grantIfAbsent(grocery, sack, UnitUsage.PURCHASE);
    }

    // ── Quick-add at the till ─────────────────────────────────────────────

    @Test
    void quickAddFilesAnUnknownScanUnderTheDefaultCategory() {
        Category general = new Category();
        general.setId(1L);
        general.setName("General");
        when(categoryRepository.findByDefaultCategoryIsTrue()).thenReturn(Optional.of(general));
        when(resolver.category(1L)).thenReturn(general);

        ProductSellingUnitResponse ready = new ProductSellingUnitResponse();
        ready.setDefault(true);
        ready.setProductId(99L);
        ProductDetailResponse detail = new ProductDetailResponse();
        detail.setSellingUnits(List.of(ready));
        when(productMapper.toDetail(any(Product.class))).thenReturn(detail);

        ProductSellingUnitResponse result = service.quickAdd(new QuickAddRequest(
                "Mystery Biscuit", new BigDecimal("30"), "8901234567890", null, null));

        // Answers in the shape a successful scan would have, so the sale in
        // progress can carry straight on.
        assertEquals(99L, result.getProductId());
        assertTrue(result.isDefault());

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(saved.capture());
        assertEquals(general, saved.getValue().getCategory());
        assertEquals("8901234567890", captureSold().getBarcode());
    }

    @Test
    void quickAddSaysSoWhenThereIsNowhereToFileThings() {
        when(categoryRepository.findByDefaultCategoryIsTrue()).thenReturn(Optional.empty());

        BusinessException thrown = assertThrows(BusinessException.class, () -> service.quickAdd(
                new QuickAddRequest("Mystery Biscuit", new BigDecimal("30"), null, null, null)));

        assertTrue(thrown.getMessage().contains("default category"));
    }

    // ── Fixtures ──────────────────────────────────────────────────────────

    private ProductCreateRequest base(String name) {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setName(name);
        request.setCategoryId(7L);
        return request;
    }

    private static ProductUnitLineRequest sellLine(String unit, String price, boolean isDefault) {
        return new ProductUnitLineRequest(unit, null, null, null, new BigDecimal(price),
                null, null, null, null, isDefault ? Boolean.TRUE : null);
    }

    private ProductSellingUnit captureSold() {
        return captureAllSold().get(0);
    }

    private List<ProductSellingUnit> captureAllSold() {
        ArgumentCaptor<ProductSellingUnit> captor = ArgumentCaptor.forClass(ProductSellingUnit.class);
        verify(sellingUnitRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    private ProductPurchaseUnit captureBought() {
        ArgumentCaptor<ProductPurchaseUnit> captor = ArgumentCaptor.forClass(ProductPurchaseUnit.class);
        verify(purchaseUnitRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().get(0);
    }

    private static Unit unit(Long id, String name, String symbol, MeasurementType type, boolean reference) {
        Unit unit = new Unit();
        unit.setId(id);
        unit.setName(name);
        unit.setSymbol(symbol);
        unit.setMeasurementType(type);
        unit.setReferenceUnit(reference);
        return unit;
    }
}
