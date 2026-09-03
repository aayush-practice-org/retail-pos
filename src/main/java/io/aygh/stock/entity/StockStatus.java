package io.aygh.stock.entity;

/**
 * How a product's stock reads at a glance. Derived from the quantity and the
 * reorder level every time it is asked for, never stored — a stored status is
 * one more thing that can disagree with the number beside it.
 */
public enum StockStatus {
    IN_STOCK,
    LOW,
    OUT_OF_STOCK
}
