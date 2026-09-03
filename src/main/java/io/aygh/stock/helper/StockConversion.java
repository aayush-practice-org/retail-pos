package io.aygh.stock.helper;

import io.aygh.exception.BusinessException;
import io.aygh.inventory.entity.Product;
import io.aygh.inventory.entity.Unit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Turning what someone typed into the base units the ledger is kept in.
 * <p>
 * Two different conversions live in the mart and they are not interchangeable:
 * <ul>
 *   <li>a <em>trading</em> unit — a purchase or selling unit — carries its own
 *       {@code packQuantity}, because a Sack holds whatever that product says it
 *       holds. Purchases and sales convert through that, not through here;</li>
 *   <li>a <em>dictionary</em> unit — Kilogram, Litre — has a fixed size against
 *       the reference unit of its measurement type. That is what this converts,
 *       and it is what a stock count or a write-off is entered in.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class StockConversion {

    /**
     * {@code quantity} of {@code enteredUnit}, expressed in {@code product}'s base
     * unit. A null or identical unit passes through untouched.
     */
    public BigDecimal toBaseUnits(Product product, BigDecimal quantity, Unit enteredUnit) {
        Unit baseUnit = product.getBaseUnit();

        if (enteredUnit == null || enteredUnit.getId().equals(baseUnit.getId())) {
            return quantity;
        }

        if (enteredUnit.getMeasurementType() != baseUnit.getMeasurementType()) {
            throw new BusinessException("'" + enteredUnit.getName() + "' measures "
                    + enteredUnit.getMeasurementType() + ", but '" + product.getName()
                    + "' is counted in " + baseUnit.getMeasurementType());
        }

        BigDecimal from = enteredUnit.getConversionFactor();
        BigDecimal to = baseUnit.getConversionFactor();
        if (from == null || from.signum() <= 0 || to == null || to.signum() <= 0) {
            throw new BusinessException("'" + enteredUnit.getName()
                    + "' has no usable conversion factor against '" + baseUnit.getName() + "'");
        }

        // Both factors are against the same measurement type's reference unit, so
        // their ratio is the conversion — no reference row has to be looked up.
        return quantity.multiply(from).divide(to, MathContext.DECIMAL64);
    }
}
