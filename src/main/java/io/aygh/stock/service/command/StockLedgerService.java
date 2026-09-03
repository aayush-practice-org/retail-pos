package io.aygh.stock.service.command;

import io.aygh.inventory.entity.Product;
import io.aygh.inventory.entity.Unit;
import io.aygh.stock.entity.StockMovement;
import io.aygh.stock.entity.StockMovementType;
import io.aygh.stock.entity.StockReferenceType;

import java.math.BigDecimal;

/**
 * The one way stock moves.
 * <p>
 * Purchasing, sales and the adjustment endpoints all write through here rather
 * than touching {@code ProductStock} themselves, which is what keeps the cached
 * quantity and the movement ledger in step. Nothing outside this package writes
 * either table.
 * <p>
 * Every method takes a quantity already converted to the product's base unit —
 * the caller knows whether it is holding a trading unit's pack quantity or a
 * dictionary unit, and this service should not have to guess.
 */
public interface StockLedgerService {

    /**
     * Posts one movement, moves the cached quantity, and returns the row written.
     * Must run inside the caller's transaction so a failed sale takes its stock
     * movements with it.
     *
     * @param quantityInBaseUnits positive; the movement type carries the direction
     */
    StockMovement post(Product product,
                       StockMovementType movementType,
                       BigDecimal quantityInBaseUnits,
                       BigDecimal enteredQuantity,
                       Unit enteredUnit,
                       Long referenceId,
                       StockReferenceType referenceType,
                       String remark);

    /**
     * Whether {@code product} has at least {@code quantityInBaseUnits} on hand.
     * A read, so it takes no lock — use it to fail a basket early with a readable
     * message; {@link #post} is what actually holds under a race.
     */
    boolean hasAvailable(Product product, BigDecimal quantityInBaseUnits);
}
