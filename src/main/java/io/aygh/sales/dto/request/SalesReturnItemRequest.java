package io.aygh.sales.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * One bill line handed back, in the selling unit it was sold in.
 */
public record SalesReturnItemRequest(

        @NotNull(message = "The bill line being returned is required")
        Long saleItemId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
        BigDecimal quantity
) {
}
