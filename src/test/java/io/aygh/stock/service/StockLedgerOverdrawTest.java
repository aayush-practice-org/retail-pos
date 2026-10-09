package io.aygh.stock.service;

import io.aygh.exception.BusinessException;
import io.aygh.inventory.entity.Product;
import io.aygh.inventory.entity.Unit;
import io.aygh.stock.entity.ProductStock;
import io.aygh.stock.entity.StockMovement;
import io.aygh.stock.entity.StockMovementType;
import io.aygh.stock.entity.StockReferenceType;
import io.aygh.stock.helper.StockResolver;
import io.aygh.stock.repository.ProductStockRepository;
import io.aygh.stock.repository.StockMovementRepository;
import io.aygh.stock.service.command.impl.StockLedgerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Which movements may take stock below zero.
 * <p>
 * A sale may — the till never refuses goods the customer is holding — and
 * nothing else does, so a negative figure only ever means "sold before it was
 * bought in".
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockLedgerOverdrawTest {

    @Mock private ProductStockRepository productStockRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private StockResolver resolver;

    private StockLedgerServiceImpl ledger;

    private Product rice;
    private ProductStock stock;

    @BeforeEach
    void setUp() {
        ledger = new StockLedgerServiceImpl(productStockRepository, stockMovementRepository, resolver);

        Unit kg = new Unit();
        kg.setSymbol("kg");

        rice = new Product();
        rice.setName("Basmati");
        rice.setBaseUnit(kg);

        stock = ProductStock.builder().product(rice).quantity(new BigDecimal("2")).build();

        when(resolver.lockFor(rice)).thenReturn(stock);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void aSaleOfMoreThanIsOnHandTakesStockBelowZero() {
        StockMovement movement = post(StockMovementType.SALE_OUT, "5");

        assertEquals(0, new BigDecimal("-3").compareTo(stock.getQuantity()));
        assertEquals(0, new BigDecimal("-3").compareTo(movement.getBalanceAfter()));
        verify(productStockRepository).save(stock);
    }

    @Test
    void aSaleFromStockAlreadyBelowZeroGoesFurtherDown() {
        stock.setQuantity(new BigDecimal("-3"));

        post(StockMovementType.SALE_OUT, "1");

        assertEquals(0, new BigDecimal("-4").compareTo(stock.getQuantity()));
    }

    @Test
    void aPurchaseBringsNegativeStockBackUp() {
        stock.setQuantity(new BigDecimal("-3"));

        post(StockMovementType.PURCHASE_IN, "10");

        assertEquals(0, new BigDecimal("7").compareTo(stock.getQuantity()));
    }

    @Test
    void everyOtherOutflowIsStillRefusedWhenItWouldOverdraw() {
        for (StockMovementType type : new StockMovementType[]{
                StockMovementType.PURCHASE_RETURN_OUT,
                StockMovementType.ADJUSTMENT_OUT,
                StockMovementType.WRITE_OFF}) {

            assertThrows(BusinessException.class, () -> post(type, "5"), type.name());
        }

        assertEquals(0, new BigDecimal("2").compareTo(stock.getQuantity()));
        verify(stockMovementRepository, never()).save(any(StockMovement.class));
    }

    private StockMovement post(StockMovementType type, String quantity) {
        return ledger.post(rice, type, new BigDecimal(quantity), new BigDecimal(quantity),
                null, 1L, StockReferenceType.SALE, null);
    }
}
