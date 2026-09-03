package io.aygh.sales.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * One line rung up.
 * <p>
 * {@code sellingUnitId} may be left out — the product's default selling unit is
 * used, which is what a barcode scan resolves to. {@code rate} may be left out
 * too, and then the unit's configured price applies; supplying one is how a
 * negotiated price is recorded, and that is what gets stored and printed.
 */
public record SaleItemRequest(

        @NotNull(message = "Product is required")
        Long productId,

        Long sellingUnitId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @DecimalMin(value = "0.0", message = "Rate cannot be negative")
        BigDecimal rate,

        @DecimalMin(value = "0.0", message = "Discount cannot be negative")
        BigDecimal discountAmount
) {
}
