package io.aygh.sales.dto.response;

import java.math.BigDecimal;

/** One line of a bill, as printed. */
public record SaleItemResponse(
        Long id,
        Long productId,
        String productName,
        String productCode,
        Long sellingUnitId,
        String unitSymbol,
        BigDecimal quantity,
        BigDecimal quantityInBaseUnits,
        BigDecimal rate,
        BigDecimal mrp,
        BigDecimal discountAmount,
        BigDecimal lineTotal
) {
}
