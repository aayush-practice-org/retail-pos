package io.aygh.catalog.dto.response;

import io.aygh.unit.entity.UnitSource;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record ProductVariantResponse(
        Long id,
        Long productId,
        String name,
        String sku,
        String barcode,
        BigDecimal packSize,
        String baseUnitSymbol,
        BigDecimal originalQuantity,
        UUID originalUnitId,
        UnitSource originalUnitSource,
        BigDecimal mrp,
        BigDecimal sellingPrice,
        boolean isDefault,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}
