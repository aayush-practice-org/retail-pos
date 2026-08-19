package io.aygh.catalog.dto.request;

import io.aygh.unit.entity.UnitSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductVariantRequest(
        @NotBlank(message = "Variant name is required")
        @Size(max = 100, message = "Variant name must not exceed 100 characters")
        String name,

        @Size(max = 64, message = "SKU must not exceed 64 characters")
        String sku,

        @Size(max = 64, message = "Barcode must not exceed 64 characters")
        String barcode,

        @NotNull(message = "Pack size is required")
        @Positive(message = "Pack size must be positive")
        BigDecimal packSize,

        // Unit the pack size is expressed in. Leave null to state the pack size
        // directly in the product's base unit; otherwise it is converted on the
        // way in — "1" + Kilogram becomes 1000 for a product tracked in grams.
        UUID packUnitId,

        UnitSource packUnitSource,

        @PositiveOrZero(message = "MRP must be zero or positive")
        BigDecimal mrp,

        @NotNull(message = "Selling price is required")
        @PositiveOrZero(message = "Selling price must be zero or positive")
        BigDecimal sellingPrice,

        boolean isDefault,

        Boolean isActive
) {

    public ProductVariantRequest {
        isActive = isActive == null || isActive;
    }
}
