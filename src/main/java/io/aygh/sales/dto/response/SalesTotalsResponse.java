package io.aygh.sales.dto.response;

import java.math.BigDecimal;

/**
 * What was sold over a window — the figures a sales dashboard opens with.
 * <p>
 * Built straight from an aggregate query, and {@code SUM} over no rows is null,
 * so a window with no sales would otherwise arrive as six nulls. The constructor
 * normalises them to zero: "nothing was sold" is a number, not a missing value.
 */
public record SalesTotalsResponse(
        long billCount,
        BigDecimal grossSales,
        BigDecimal discountGiven,
        BigDecimal vatCollected,
        BigDecimal netSales,
        BigDecimal collected,
        BigDecimal outstanding
) {

    public SalesTotalsResponse {
        grossSales = zeroIfNull(grossSales);
        discountGiven = zeroIfNull(discountGiven);
        vatCollected = zeroIfNull(vatCollected);
        netSales = zeroIfNull(netSales);
        collected = zeroIfNull(collected);
        outstanding = zeroIfNull(outstanding);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
