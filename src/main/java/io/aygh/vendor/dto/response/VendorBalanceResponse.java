package io.aygh.vendor.dto.response;

import io.aygh.vendor.entity.BalanceType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One posted ledger entry. The amount is as it was recorded — positive — and
 * {@code balanceType} is what says which direction it moved the account.
 */
public record VendorBalanceResponse(
        Long id,
        Long vendorId,
        BigDecimal amount,
        BalanceType balanceType,
        Instant createdAt
) {
}
