package io.aygh.vendor.dto.response;

import io.aygh.vendor.entity.BalanceType;

import java.math.BigDecimal;

/**
 * Where a vendor's account stands, with the three running totals it was derived
 * from so a disputed figure can be traced without re-querying.
 * <p>
 * {@code outstanding} is always positive and {@code balanceType} says who is
 * behind: {@link BalanceType#PAYABLE} when the mart still owes the vendor,
 * {@link BalanceType#RECEIVABLE} when it has overpaid and is owed money back.
 * A settled account reports zero against {@code PAYABLE}.
 */
public record VendorBalanceSummaryResponse(
        Long vendorId,
        BigDecimal outstanding,
        BalanceType balanceType,
        BigDecimal totalPayable,
        BigDecimal totalReceivable,
        BigDecimal totalSettled
) {
}
