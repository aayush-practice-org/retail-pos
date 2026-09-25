package io.aygh.stock.entity;

/**
 * What kind of document a movement points back at, so a
 * {@code referenceId} can be resolved without guessing which table it belongs
 * to. Null for a movement raised on its own.
 */
public enum StockReferenceType {
    PURCHASE,
    /** A debit note: goods sent back to the vendor. */
    PURCHASE_RETURN,
    SALE,
    /** A credit note: goods handed back against a sale. */
    SALE_RETURN,
    ADJUSTMENT,
    WRITE_OFF
}
