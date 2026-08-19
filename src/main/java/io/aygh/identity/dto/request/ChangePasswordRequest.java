package io.aygh.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String oldPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 characters long")
        String newPassword
) {
}
