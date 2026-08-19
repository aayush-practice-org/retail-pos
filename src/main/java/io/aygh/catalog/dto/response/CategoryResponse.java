package io.aygh.catalog.dto.response;

import java.time.Instant;
import java.util.List;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        Long parentId,
        List<CategoryResponse> children,
        Instant createdAt,
        Instant updatedAt
) {
}
