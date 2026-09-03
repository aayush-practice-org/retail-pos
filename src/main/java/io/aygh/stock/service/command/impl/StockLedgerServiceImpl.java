package io.aygh.stock.service.command.impl;

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
import io.aygh.stock.service.command.StockLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockLedgerServiceImpl implements StockLedgerService {

    private final ProductStockRepository productStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockResolver resolver;

    /**
     * {@code MANDATORY}: a movement only means anything alongside the document
     * that caused it. Posting one in its own transaction would leave stock
     * deducted for a sale that then failed to save.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement post(Product product,
                              StockMovementType movementType,
                              BigDecimal quantityInBaseUnits,
                              BigDecimal enteredQuantity,
                              Unit enteredUnit,
                              Long referenceId,
                              StockReferenceType referenceType,
                              String remark) {

        if (quantityInBaseUnits == null || quantityInBaseUnits.signum() <= 0) {
            throw new BusinessException("A stock movement must carry a quantity greater than zero");
        }

        ProductStock stock = resolver.lockFor(product);
        BigDecimal change = quantityInBaseUnits.multiply(BigDecimal.valueOf(movementType.direction()));
        BigDecimal balanceAfter = stock.getQuantity().add(change);

        // Checked against the locked row rather than the caller's earlier read:
        // this is the point at which two tills selling the last item are ordered.
        if (balanceAfter.signum() < 0) {
            throw new BusinessException("'" + product.getName() + "' has only "
                    + stock.getQuantity().stripTrailingZeros().toPlainString() + " "
                    + product.getBaseUnit().getSymbol() + " on hand");
        }

        stock.setQuantity(balanceAfter);
        productStockRepository.save(stock);

        StockMovement movement = stockMovementRepository.save(StockMovement.builder()
                .product(product)
                .movementType(movementType)
                .quantity(quantityInBaseUnits)
                .balanceAfter(balanceAfter)
                .enteredQuantity(enteredQuantity)
                .enteredUnit(enteredUnit)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .remark(remark)
                .build());

        log.info("Stock {} {} {} of '{}' — balance now {}",
                movementType, movementType.isInflow() ? "+" : "-",
                quantityInBaseUnits, product.getName(), balanceAfter);

        return movement;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAvailable(Product product, BigDecimal quantityInBaseUnits) {
        return resolver.stockOf(product.getId()).covers(quantityInBaseUnits);
    }
}
