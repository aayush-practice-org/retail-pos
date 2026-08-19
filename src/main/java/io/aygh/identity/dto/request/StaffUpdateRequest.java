package io.aygh.identity.dto.request;

import io.aygh.identity.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Every field is optional — only what is supplied gets changed.
 */
public record StaffUpdateRequest(
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @Email(message = "Invalid email format")
        String email,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Size(min = 6, message = "Password must be at least 6 characters long")
        String password,

        UserRole role,

        Boolean isActive
) {
}
