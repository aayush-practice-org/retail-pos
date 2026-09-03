package io.aygh.shared.entity;

/**
 * How much of a document has actually been paid.
 * <p>
 * Derived from the amounts rather than set by hand — see
 * {@code Sale#settle} — so it can never disagree with them.
 */
public enum PaymentStatus {

    UNPAID,

    /** Something was paid, but less than the net payable. */
    PARTIAL,

    PAID
}
