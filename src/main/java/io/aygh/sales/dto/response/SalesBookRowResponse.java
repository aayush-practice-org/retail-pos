package io.aygh.sales.dto.response;

import java.math.BigDecimal;

/** One bill as it appears on a line of the IRD sales book. */
public record SalesBookRowResponse(
        String date,
        String billNumber,
        String buyerName,
        String buyerPan,
        BigDecimal totalSales,
        BigDecimal nonTaxableSales,
        BigDecimal exportSales,
        BigDecimal discount,
        BigDecimal taxableAmount,
        BigDecimal tax
) {
}
