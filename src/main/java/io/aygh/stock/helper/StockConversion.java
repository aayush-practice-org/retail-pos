package io.aygh.stock.helper;

import io.aygh.exception.BusinessException;
import io.aygh.inventory.entity.Product;
import io.aygh.inventory.entity.Unit;
import io.aygh.inventory.repository.ProductPurchaseUnitRepository;
import io.aygh.inventory.repository.ProductSellingUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Turning what someone typed into the base units the ledger is kept in.
 * <p>
 * If entered in the base unit, passes through as-is.
 * Otherwise, converts using the product's configured pack quantity for that unit.
 */
@Component
@RequiredArgsConstructor
public class StockConversion {

    private final ProductPurchaseUnitRepository purchaseUnitRepository;
    private final ProductSellingUnitRepository sellingUnitRepository;

    /**
     * {@code quantity} of {@code enteredUnit}, expressed in {@code product}'s base
     * unit. A null or identical unit passes through untouched.
     */
    public BigDecimal toBaseUnits(Product product, BigDecimal quantity, Unit enteredUnit) {
        Unit baseUnit = product.getBaseUnit();

        if (enteredUnit == null || enteredUnit.getId().equals(baseUnit.getId())) {
            return quantity;
        }

        // Try product purchase unit configuration
        var purchaseUnit = purchaseUnitRepository.findByProductIdAndUnitId(product.getId(), enteredUnit.getId());
        if (purchaseUnit.isPresent()) {
            return quantity.multiply(purchaseUnit.get().getPackQuantity());
        }

        // Try product selling unit configuration
        var sellingUnit = sellingUnitRepository.findByProductIdAndUnitId(product.getId(), enteredUnit.getId());
        if (sellingUnit.isPresent()) {
            return quantity.multiply(sellingUnit.get().getPackQuantity());
        }

        throw new BusinessException("'" + enteredUnit.getName() + "' is not configured for '"
                + product.getName() + "'. Adjustments must be in the base unit ('"
                + baseUnit.getName() + "') or a configured purchase/selling unit");
    }
}
