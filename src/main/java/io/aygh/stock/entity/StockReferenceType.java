package io.aygh.stock.entity;

/**
 * What kind of document a movement points back at, so a
 * {@code referenceId} can be resolved without guessing which table it belongs
 * to. Null for a movement raised on its own.
 */
public enum StockReferenceType {
    PURCHASE,
    SALE,
    ADJUSTMENT,
    WRITE_OFF
}
