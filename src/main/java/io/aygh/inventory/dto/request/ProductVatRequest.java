package io.aygh.inventory.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Opens a new VAT rate. There is no update: a rate that was wrong for a period
 * is corrected by closing it and opening another, so the record of what was
 * charged stays intact.
 */
public record ProductVatRequest(

        @NotNull(message = "Rate is required")
        @DecimalMin(value = "0.00", message = "Rate cannot be negative")
        @DecimalMax(value = "100.00", message = "Rate cannot exceed 100%")
        BigDecimal rate
) {
}
