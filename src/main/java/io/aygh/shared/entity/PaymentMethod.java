package io.aygh.shared.entity;

/**
 * How money changed hands, on either side of the trade.
 * <p>
 * Shared between purchasing and sales because it is the same question in both
 * directions, and because a report that adds up "what went out in cash today"
 * cannot do it against two enums that happen to spell the constants the same.
 */
public enum PaymentMethod {

    CASH,
    CARD,

    /** Bank transfer, cheque, or any other settled-outside-the-till method. */
    BANK,

    /** A wallet or QR rail — eSewa, Khalti, FonePay. */
    WALLET,

    /**
     * Nothing changed hands yet. On a purchase this raises a payable against the
     * vendor; on a sale it leaves the invoice unpaid.
     */
    CREDIT
}
