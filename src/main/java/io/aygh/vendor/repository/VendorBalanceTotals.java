package io.aygh.vendor.repository;

import java.math.BigDecimal;

/**
 * The three running totals of a vendor's ledger, read in one aggregate query.
 * <p>
 * A vendor with no entries at all still produces a row — of nulls, because
 * {@code SUM} over nothing is null — so the constructor normalises them. Callers
 * can then do arithmetic without a null check on every field.
 */
public record VendorBalanceTotals(
        BigDecimal payable,
        BigDecimal receivable,
        BigDecimal settled
) {

    public VendorBalanceTotals {
        payable = payable == null ? BigDecimal.ZERO : payable;
        receivable = receivable == null ? BigDecimal.ZERO : receivable;
        settled = settled == null ? BigDecimal.ZERO : settled;
    }

    /**
     * What the mart still owes. Negative when the vendor has been overpaid —
     * the summary turns that around into a receivable before it goes out.
     */
    public BigDecimal outstanding() {
        return payable.subtract(receivable).subtract(settled);
    }
}
