package io.aygh.sales.dto.response;

import java.math.BigDecimal;

/** The sales book's closing "Total amount" line. */
public record SalesBookTotalResponse(
        BigDecimal totalSales,
        BigDecimal nonTaxableSales,
        BigDecimal exportSales,
        BigDecimal discount,
        BigDecimal taxableAmount,
        BigDecimal tax
) {
}
