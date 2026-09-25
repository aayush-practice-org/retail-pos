package io.aygh.sales.dto.response;

import java.math.BigDecimal;

/** One line of a credit note. */
public record SalesReturnItemResponse(
        Long id,
        Long saleItemId,
        Long productId,
        String productName,
        String unitSymbol,
        BigDecimal quantity,
        BigDecimal quantityInBaseUnits,
        BigDecimal rate,
        BigDecimal lineTotal
) {
}
