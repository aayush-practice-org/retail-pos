package io.aygh.unit.dto.request;

import io.aygh.unit.entity.MeasurementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateCustomUnitRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Symbol is required")
    @Size(max = 20, message = "Symbol must not exceed 20 characters")
    private String symbol;

    @NotNull(message = "Measurement type is required")
    private MeasurementType measurementType;

    @NotNull(message = "Conversion factor is required")
    @Positive(message = "Conversion factor must be positive")
    private BigDecimal conversionFactor;
}
