package io.aygh.inventory.service;

import io.aygh.exception.BusinessException;
import io.aygh.inventory.dto.request.ProductCreateRequest;
import io.aygh.inventory.dto.response.ProductImportResponse;
import io.aygh.inventory.entity.Category;
import io.aygh.inventory.entity.MeasurementType;
import io.aygh.inventory.entity.Unit;
import io.aygh.inventory.helper.ProductUnitFactory;
import io.aygh.inventory.repository.CategoryRepository;
import io.aygh.inventory.repository.ProductRepository;
import io.aygh.inventory.repository.ProductSellingUnitRepository;
import io.aygh.inventory.service.command.ProductCommandService;
import io.aygh.inventory.service.command.impl.ProductImportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Importing a catalogue. The behaviour that matters is what happens to a file
 * that is partly wrong, because in practice every first import is.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductImportTest {

    @Mock private ProductCommandService productCommandService;
    @Mock private ProductUnitFactory unitFactory;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductSellingUnitRepository sellingUnitRepository;

    private ProductImportServiceImpl service;

    private Category general;
    private Category beverages;

    @BeforeEach
    void setUp() {
        service = new ProductImportServiceImpl(
                productCommandService, unitFactory, categoryRepository,
                productRepository, sellingUnitRepository);

        general = category(1L, "General");
        beverages = category(2L, "Beverages");

        when(categoryRepository.findByDefaultCategoryIsTrue()).thenReturn(Optional.of(general));
        when(categoryRepository.findByNameIgnoreCase("Beverages")).thenReturn(Optional.of(beverages));
        when(categoryRepository.findByNameIgnoreCase("Nonsense")).thenReturn(Optional.empty());

        Unit kilogram = new Unit();
        kilogram.setId(2L);
        kilogram.setName("Kilogram");
        kilogram.setSymbol("kg");
        kilogram.setMeasurementType(MeasurementType.WEIGHT);
        when(unitFactory.resolve(isNull(), eq("kg"))).thenReturn(kilogram);
        when(unitFactory.resolve(isNull(), eq("furlong")))
                .thenThrow(new BusinessException("No unit called 'furlong'"));
    }

    // ── The good case ─────────────────────────────────────────────────────

    @Test
    void aCleanFileIsImported() {
        ProductImportResponse report = importing("""
                name,category,barcode,buy_price,sell_price
                Wai Wai Noodles,Beverages,8901234567890,20,25
                Coca-Cola 500ml,Beverages,8901234567891,45,55
                """);

        assertTrue(report.clean());
        assertEquals(2, report.rows());
        assertEquals(2, report.imported());
        verify(productCommandService, times(2)).create(any());
    }

    @Test
    void aRowWithNoCategoryGoesToTheDefault() {
        importing("""
                name,sell_price
                Wai Wai Noodles,25
                """);

        assertEquals(1L, captureCreated().get(0).getCategoryId());
    }

    @Test
    void pricesAndUnitsBecomeTradingLines() {
        importing("""
                name,sell_unit,sell_price,buy_price,pack_qty,pack_unit,vat_rate
                Basmati Rice,kg,145,5800,50,kg,13
                """);

        ProductCreateRequest created = captureCreated().get(0);
        assertEquals("kg", created.getSellIn().get(0).unit());
        assertEquals(0, new BigDecimal("145").compareTo(created.getSellIn().get(0).price()));
        assertEquals(0, new BigDecimal("50").compareTo(created.getBuyIn().get(0).contains().qty()));
        assertEquals("kg", created.getBuyIn().get(0).contains().unit());
        assertEquals(0, new BigDecimal("13").compareTo(created.getBuyIn().get(0).vatRate()));
    }

    @Test
    void thousandsSeparatorsAreTolerated() {
        importing("""
                name,sell_price
                Basmati Rice,"5,800"
                """);

        assertEquals(0, new BigDecimal("5800")
                .compareTo(captureCreated().get(0).getSellIn().get(0).price()));
    }

    @Test
    void aRowWithNoBuyPriceGetsNoPurchaseLine() {
        importing("""
                name,sell_price
                Wai Wai Noodles,25
                """);

        assertNull(captureCreated().get(0).getBuyIn());
    }

    // ── Reporting ─────────────────────────────────────────────────────────

    @Test
    void everyProblemIsReportedAtOnce() {
        // The point of the pre-flight pass: four bad rows, one upload.
        ProductImportResponse report = importing("""
                name,category,sell_price,sell_unit
                Good Product,Beverages,25,
                ,Beverages,30,
                Bad Number,Beverages,abc,
                Bad Category,Nonsense,25,
                Bad Unit,Beverages,25,furlong
                """);

        assertFalse(report.clean());
        assertEquals(5, report.rows());
        assertEquals(0, report.imported());
        assertEquals(4, report.errors().size());

        assertEquals(3, report.errors().get(0).line(), "line numbers count the header");
        assertTrue(report.errors().get(1).message().contains("not a number"));
        assertTrue(report.errors().get(2).message().contains("Nonsense"));
        assertTrue(report.errors().get(3).message().contains("furlong"));
    }

    @Test
    void nothingIsWrittenWhenAnyRowIsBad() {
        importing("""
                name,sell_price
                Good Product,25
                Bad Number,abc
                """);

        verify(productCommandService, never()).create(any());
    }

    @Test
    void aBarcodeRepeatedInTheFileIsCaught() {
        ProductImportResponse report = importing("""
                name,barcode,sell_price
                First,8901234567890,25
                Second,8901234567890,30
                """);

        assertEquals(1, report.errors().size());
        assertTrue(report.errors().get(0).message().contains("twice in the file"));
        assertEquals(3, report.errors().get(0).line());
    }

    @Test
    void aBarcodeTheCatalogueAlreadyHoldsIsCaught() {
        when(sellingUnitRepository.existsByBarcode("8901234567890")).thenReturn(true);

        ProductImportResponse report = importing("""
                name,barcode,sell_price
                First,8901234567890,25
                """);

        assertTrue(report.errors().get(0).message().contains("already assigned"));
    }

    @Test
    void aNameRepeatedWithinOneCategoryIsCaught() {
        ProductImportResponse report = importing("""
                name,category,sell_price
                Sugar,Beverages,25
                Sugar,Beverages,30
                """);

        assertEquals(1, report.errors().size());
        assertTrue(report.errors().get(0).message().contains("more than once"));
    }

    @Test
    void theSameNameInDifferentCategoriesIsFine() {
        ProductImportResponse report = importing("""
                name,category,sell_price
                Sugar,Beverages,25
                Sugar,,30
                """);

        assertTrue(report.clean(), () -> report.errors().toString());
    }

    @Test
    void aNameTheCatalogueAlreadyHoldsIsCaught() {
        when(productRepository.existsByNameIgnoreCaseAndCategoryId(eq("Sugar"), eq(2L))).thenReturn(true);

        ProductImportResponse report = importing("""
                name,category,sell_price
                Sugar,Beverages,25
                """);

        assertTrue(report.errors().get(0).message().contains("already in 'Beverages'"));
    }

    // ── Dry run ───────────────────────────────────────────────────────────

    @Test
    void aDryRunChecksWithoutWriting() {
        ProductImportResponse report = service.importFrom(file("""
                name,sell_price
                Wai Wai Noodles,25
                """), true);

        assertTrue(report.clean());
        assertTrue(report.dryRun());
        assertEquals(0, report.imported());
        verify(productCommandService, never()).create(any());
    }

    @Test
    void aDryRunOnABadFileStillReports() {
        ProductImportResponse report = service.importFrom(file("""
                name,sell_price
                Bad Number,abc
                """), true);

        assertFalse(report.clean());
        assertTrue(report.dryRun());
    }

    // ── Files that are not usable at all ──────────────────────────────────

    @Test
    void aFileWithoutTheRequiredColumnsIsRefusedOutright() {
        BusinessException thrown = assertThrows(BusinessException.class,
                () -> importing("product,cost\nSugar,25\n"));

        assertTrue(thrown.getMessage().contains("sell_price"));
    }

    @Test
    void aHeaderWithNoRowsIsRefused() {
        BusinessException thrown = assertThrows(BusinessException.class,
                () -> importing("name,sell_price\n"));

        assertTrue(thrown.getMessage().contains("no rows"));
    }

    @Test
    void anEmptyUploadIsRefused() {
        assertThrows(BusinessException.class, () -> service.importFrom(
                new MockMultipartFile("file", "products.csv", "text/csv", new byte[0]), false));
    }

    // ── Fixtures ──────────────────────────────────────────────────────────

    private ProductImportResponse importing(String csv) {
        return service.importFrom(file(csv), false);
    }

    private static MockMultipartFile file(String csv) {
        return new MockMultipartFile(
                "file", "products.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
    }

    private List<ProductCreateRequest> captureCreated() {
        ArgumentCaptor<ProductCreateRequest> captor = ArgumentCaptor.forClass(ProductCreateRequest.class);
        verify(productCommandService, atLeastOnce()).create(captor.capture());
        return captor.getAllValues();
    }

    private static Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }
}
