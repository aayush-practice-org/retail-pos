package io.aygh.stock.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** The level, in the product's base unit, at or below which it reads as low. */
public record ReorderLevelRequest(

        @NotNull(message = "Reorder level is required")
        @DecimalMin(value = "0.0", message = "Reorder level cannot be negative")
        BigDecimal reorderLevel
) {
}
