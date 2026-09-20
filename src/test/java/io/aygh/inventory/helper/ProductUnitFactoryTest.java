package io.aygh.inventory.helper;

import io.aygh.exception.BusinessException;
import io.aygh.inventory.dto.request.PackSizeRequest;
import io.aygh.inventory.dto.request.ProductUnitLineRequest;
import io.aygh.inventory.entity.MeasurementType;
import io.aygh.inventory.entity.Unit;
import io.aygh.inventory.repository.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * The arithmetic that decides what a pack holds. Worth pinning down: every
 * quantity the stock ledger ever records passes through it, and a wrong answer
 * here is not visible until someone counts the shelf.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductUnitFactoryTest {

    @Mock
    private UnitRepository unitRepository;

    private ProductUnitFactory factory;

    private Unit gram;
    private Unit kilogram;
    private Unit sack;
    private Unit piece;
    private Unit litre;

    @BeforeEach
    void setUp() {
        factory = new ProductUnitFactory(unitRepository);

        gram = unit(1L, "Gram", "g", MeasurementType.WEIGHT, BigDecimal.ONE, true);
        kilogram = unit(2L, "Kilogram", "kg", MeasurementType.WEIGHT, new BigDecimal("1000"), false);
        sack = unit(3L, "Sack", "sack", MeasurementType.WEIGHT, null, false);
        piece = unit(4L, "Piece", "pc", MeasurementType.COUNT, BigDecimal.ONE, true);
        litre = unit(5L, "Liter", "l", MeasurementType.VOLUME, new BigDecimal("1000"), false);

        List<Unit> dictionary = List.of(gram, kilogram, sack, piece, litre);

        // Stand-ins for the real queries, matching case-insensitively as the
        // "IgnoreCase" in their names promises. Stubbing them exact-match would
        // make the test pass or fail on the fake's behaviour rather than the
        // factory's, and the trimming below is the factory's job to get right.
        lenient().when(unitRepository.findById(anyLong())).thenAnswer(call ->
                dictionary.stream()
                        .filter(unit -> unit.getId().equals(call.getArgument(0)))
                        .findFirst());
        lenient().when(unitRepository.findBySymbolIgnoreCase(anyString())).thenAnswer(call ->
                dictionary.stream()
                        .filter(unit -> unit.getSymbol().equalsIgnoreCase(call.getArgument(0)))
                        .findFirst());
        lenient().when(unitRepository.findByNameIgnoreCase(anyString())).thenAnswer(call ->
                dictionary.stream()
                        .filter(unit -> unit.getName().equalsIgnoreCase(call.getArgument(0)))
                        .findFirst());
    }

    // ── Pack quantity ─────────────────────────────────────────────────────

    @Test
    void unitWithAFixedSizeNeedsNoPackQuantity() {
        // Selling by the kilogram, counted in grams: the unit already knows.
        assertEquals(0, new BigDecimal("1000")
                .compareTo(factory.packQuantity(gram, kilogram, line(null, null))));
    }

    @Test
    void theBaseUnitIsAlwaysOne() {
        assertEquals(0, BigDecimal.ONE.compareTo(factory.packQuantity(gram, gram, line(null, null))));
    }

    @Test
    void packSizeIsConvertedFromTheUnitAShopkeeperWouldSayIt() {
        // "a sack holds 50 kg", counted in grams, is 50000 — and nobody typed it.
        BigDecimal packed = factory.packQuantity(
                gram, sack, line(null, new PackSizeRequest(new BigDecimal("50"), "kg", null)));

        assertEquals(0, new BigDecimal("50000").compareTo(packed));
    }

    @Test
    void anExplicitPackQuantityWinsOverEverythingElse() {
        BigDecimal packed = factory.packQuantity(
                gram, sack,
                line(new BigDecimal("25000"), new PackSizeRequest(new BigDecimal("50"), "kg", null)));

        assertEquals(0, new BigDecimal("25000").compareTo(packed));
    }

    @Test
    void aUnitWithNoFixedSizeAndNoPackSizeIsRefused() {
        // The important one: a Sack must never silently default to one gram.
        BusinessException thrown = assertThrows(BusinessException.class,
                () -> factory.packQuantity(gram, sack, line(null, null)));

        assertTrue(thrown.getMessage().contains("Sack"));
        assertTrue(thrown.getMessage().contains("no fixed size"));
    }

    @Test
    void aPackSizeInTheWrongDimensionIsRefused() {
        BusinessException thrown = assertThrows(BusinessException.class,
                () -> factory.packQuantity(
                        gram, sack, line(null, new PackSizeRequest(new BigDecimal("2"), "l", null))));

        assertTrue(thrown.getMessage().contains("VOLUME"));
    }

    // ── Base unit derivation ──────────────────────────────────────────────

    @Test
    void stockIsCountedInTheReferenceUnitOfItsMeasurementType() {
        when(unitRepository.findByMeasurementTypeAndReferenceUnitIsTrue(MeasurementType.WEIGHT))
                .thenReturn(Optional.of(gram));

        assertEquals(gram, factory.baseUnitFor(MeasurementType.WEIGHT));
    }

    @Test
    void aMeasurementTypeWithNoReferenceUnitIsRefused() {
        when(unitRepository.findByMeasurementTypeAndReferenceUnitIsTrue(MeasurementType.LENGTH))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> factory.baseUnitFor(MeasurementType.LENGTH));
    }

    // ── Resolving what the caller wrote ───────────────────────────────────

    @Test
    void unitsResolveBySymbolOrName() {
        assertEquals(kilogram, factory.resolve(null, "kg"));
        assertEquals(kilogram, factory.resolve(null, "Kilogram"));
        assertEquals(kilogram, factory.resolve(null, "  KG  "));
    }

    @Test
    void namingNoUnitMeansTheBaseUnit() {
        assertNull(factory.resolve(null, null));
        assertNull(factory.resolve(null, "   "));
    }

    @Test
    void anUnknownUnitSaysSoRatherThanGuessing() {
        BusinessException thrown = assertThrows(BusinessException.class,
                () -> factory.resolve(null, "furlong"));

        assertTrue(thrown.getMessage().contains("furlong"));
    }

    // ── Fixtures ──────────────────────────────────────────────────────────

    private static Unit unit(Long id, String name, String symbol, MeasurementType type,
                             BigDecimal factor, boolean reference) {
        Unit unit = new Unit();
        unit.setId(id);
        unit.setName(name);
        unit.setSymbol(symbol);
        unit.setMeasurementType(type);
        unit.setConversionFactor(factor);
        unit.setReferenceUnit(reference);
        return unit;
    }

    private static ProductUnitLineRequest line(BigDecimal packQuantity, PackSizeRequest contains) {
        return new ProductUnitLineRequest(
                null, null, contains, packQuantity, null, null, null, null, null, null);
    }
}
