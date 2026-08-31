package io.aygh.identity.dto.request;

import io.aygh.identity.entity.UserRole;
import io.aygh.identity.entity.UserStatus;
import io.aygh.shared.entity.Gender;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;


public record StaffUpdateRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email,

        @NotNull(message = "Role is required")
        UserRole role,

        @NotNull(message = "Status is required")
        UserStatus status,

        Instant expiresAt,

        @NotBlank(message = "Full name is required")
        @Size(max = 255)
        String fullName,

        @Past(message = "Date of birth must be in the past")
        LocalDate dob,

        Gender gender,

        @Size(max = 100)
        String country,

        @Size(max = 30)
        String mobileNumber,

        @Size(max = 255)
        String addressLine1,

        @Size(max = 255)
        String addressLine2,

        @Size(max = 255)
        String city,

        @Size(max = 255)
        String state,

        @Size(max = 20)
        String zipCode
) {
}
