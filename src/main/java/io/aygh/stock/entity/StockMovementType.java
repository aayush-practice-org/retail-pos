package io.aygh.stock.entity;

/**
 * Why stock moved, and — through {@link #direction()} — which way.
 * <p>
 * Direction is a property of the type rather than a separate field a caller
 * passes: a {@code SALE_OUT} that increased stock is not a thing that should be
 * representable.
 */
public enum StockMovementType {

    PURCHASE_IN(1),
    SALE_OUT(-1),

    /** A sale reversed. Goods came back on the shelf. */
    SALE_RETURN_IN(1),

    /** Goods sent back to the vendor. */
    PURCHASE_RETURN_OUT(-1),

    /** A count found more than the books said. */
    ADJUSTMENT_IN(1),

    /** A count found less. */
    ADJUSTMENT_OUT(-1),

    /** Expired, damaged, or otherwise gone without being sold. */
    WRITE_OFF(-1);

    private final int direction;

    StockMovementType(int direction) {
        this.direction = direction;
    }

    /** {@code +1} for stock coming in, {@code -1} for stock going out. */
    public int direction() {
        return direction;
    }

    public boolean isInflow() {
        return direction > 0;
    }
}
