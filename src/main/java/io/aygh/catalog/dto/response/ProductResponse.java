package io.aygh.catalog.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ProductResponse(
        Long id,
        String name,
        String description,
        String brand,
        Long categoryId,
        String categoryName,
        UUID baseUnitId,
        String baseUnitName,
        String baseUnitSymbol,
        String imageUrl,
        boolean isActive,
        long variantCount,
        Instant createdAt,
        Instant updatedAt
) {
}
