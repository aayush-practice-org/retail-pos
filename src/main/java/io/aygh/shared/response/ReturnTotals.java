package io.aygh.shared.response;

import java.math.BigDecimal;

/**
 * Returns raised over a window — credit notes on the sales side, debit notes on
 * the purchase side. {@code SUM} over no rows is null, so it is normalised here.
 */
public record ReturnTotals(
        Long count,
        BigDecimal netTotal,
        BigDecimal vatAmount
) {

    public ReturnTotals {
        count = count == null ? 0L : count;
        netTotal = netTotal == null ? BigDecimal.ZERO : netTotal;
        vatAmount = vatAmount == null ? BigDecimal.ZERO : vatAmount;
    }
}
