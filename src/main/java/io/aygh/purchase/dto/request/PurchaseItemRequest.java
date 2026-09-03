package io.aygh.purchase.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * One line of a purchase.
 * <p>
 * {@code rate} is optional: left out, the purchase unit's configured purchase
 * price is used. A vendor who changed their price on this bill supplies it, and
 * that is what gets stored — the catalogue price is a default, not a rule.
 */
public record PurchaseItemRequest(

        @NotNull(message = "Product is required")
        Long productId,

        @NotNull(message = "Purchase unit is required")
        Long purchaseUnitId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @DecimalMin(value = "0.0", message = "Rate cannot be negative")
        BigDecimal rate
) {
}
