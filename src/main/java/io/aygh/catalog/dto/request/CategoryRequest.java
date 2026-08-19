package io.aygh.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String name,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        String imageUrl,

        Long parentId
) {
}
