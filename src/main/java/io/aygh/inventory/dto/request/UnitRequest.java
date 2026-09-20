package io.aygh.inventory.dto.request;

import io.aygh.inventory.entity.MeasurementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Create and update share one shape: a unit has nothing that is fixed at
 * creation, so splitting them would produce two identical records.
 */
public record UnitRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100)
        String name,

        @NotBlank(message = "Symbol is required")
        @Size(max = 20)
        String symbol,

        @NotNull(message = "Measurement type is required")
        MeasurementType measurementType,

        /**
         * How many of the measurement type's reference unit one of these is —
         * a Quintal is 100000 where Gram is the reference.
         * <p>
         * Left out for a unit whose size is a per-product decision: a Sack, a
         * Crate, a Carton. Those get their size from the product's pack
         * quantity instead, which is the whole reason this is optional.
         */
        @DecimalMin(value = "0.000001", message = "Conversion factor must be greater than zero")
        BigDecimal conversionFactor
) {
}
