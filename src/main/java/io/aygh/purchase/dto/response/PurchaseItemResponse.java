package io.aygh.purchase.dto.response;

import java.math.BigDecimal;

/** One line of a purchase, as it is displayed and reprinted. */
public record PurchaseItemResponse(
        Long id,
        Long productId,
        String productName,
        String productCode,
        Long purchaseUnitId,
        String purchaseUnitName,
        String purchaseUnitSymbol,
        BigDecimal quantity,
        BigDecimal packQuantity,
        BigDecimal quantityInBaseUnits,
        String baseUnitSymbol,
        BigDecimal rate,
        BigDecimal lineTotal
) {
}
