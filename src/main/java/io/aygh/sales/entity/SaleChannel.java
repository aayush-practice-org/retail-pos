package io.aygh.sales.entity;

/**
 * Where a sale was rung up. The till and the back office raise the same
 * document — the difference is only which screen it came from, and reports care
 * about that.
 */
public enum SaleChannel {

    /** Scanned through at the counter. */
    POS,

    /** Raised from the sales screens — a quote turned into an order, a phone order. */
    BACK_OFFICE
}
