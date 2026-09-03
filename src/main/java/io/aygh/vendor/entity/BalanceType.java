package io.aygh.vendor.entity;

/**
 * What one row of a vendor's ledger means.
 * <p>
 * The ledger is append-only and every entry carries a positive amount; the type
 * is what gives the amount its sign when the outstanding figure is worked out.
 * Storing a signed amount instead would make "how much have we actually paid
 * this vendor" unanswerable without re-deriving intent from the sign.
 */
public enum BalanceType {

    /** The mart owes the vendor — goods received on credit. */
    PAYABLE,

    /** The vendor owes the mart — a return, a credit note, an overpayment. */
    RECEIVABLE,

    /** Money handed over. Reduces what is outstanding without cancelling the debt it paid. */
    SETTLEMENT
}
