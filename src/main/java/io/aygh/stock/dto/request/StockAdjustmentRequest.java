package io.aygh.stock.dto.request;

import io.aygh.stock.entity.StockMovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * A stock correction entered by hand: a count that disagreed with the books, or
 * goods gone without being sold.
 * <p>
 * {@code unitId} is optional and names a dictionary unit — leave it out to enter
 * the quantity in the product's own base unit.
 */
public record StockAdjustmentRequest(

        @NotNull(message = "Product is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
        BigDecimal quantity,

        Long unitId,

        @NotNull(message = "Movement type is required")
        StockMovementType movementType,

        @Size(max = 255)
        String remark
) {
}
