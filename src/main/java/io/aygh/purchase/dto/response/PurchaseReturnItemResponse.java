package io.aygh.purchase.dto.response;

import java.math.BigDecimal;

/** One line of a debit note. */
public record PurchaseReturnItemResponse(
        Long id,
        Long purchaseItemId,
        Long productId,
        String productName,
        String unitSymbol,
        BigDecimal quantity,
        BigDecimal quantityInBaseUnits,
        BigDecimal rate,
        BigDecimal lineTotal
) {
}
