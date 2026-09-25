package io.aygh.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100)
        String name,

        @Size(max = 255)
        String description,

        String image
) {
}
