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
        MeasurementType measurementType
) {
}
