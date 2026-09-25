package io.aygh.purchase.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * One bill line sent back, in the purchase unit it was bought in.
 */
public record PurchaseReturnItemRequest(

        @NotNull(message = "The bill line being returned is required")
        Long purchaseItemId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
        BigDecimal quantity
) {
}
