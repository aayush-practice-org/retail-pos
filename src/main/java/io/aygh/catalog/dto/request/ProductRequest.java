package io.aygh.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must not exceed 255 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @Size(max = 255, message = "Brand must not exceed 255 characters")
        String brand,

        @NotNull(message = "Category id is required")
        Long categoryId,

        @NotNull(message = "Base unit id is required")
        UUID baseUnitId,

        String imageUrl,

        Boolean isActive
) {

    public ProductRequest {
        // omitted means "on the shelf" — a product should not go live switched off
        isActive = isActive == null || isActive;
    }
}
