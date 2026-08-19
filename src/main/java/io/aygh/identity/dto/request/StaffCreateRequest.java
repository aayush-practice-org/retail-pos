package io.aygh.identity.dto.request;

import io.aygh.identity.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StaffCreateRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters long")
        String password,

        @NotNull(message = "Role is required")
        UserRole role
) {
}
