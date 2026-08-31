package io.aygh.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;


public record AdminUpdateRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email,

        @Size(max = 255)
        String fullName,

        @Size(max = 30)
        String mobileNumber,

        @NotBlank(message = "Company name is required")
        @Size(max = 255)
        String companyName,

        @Size(max = 255)
        String companyAddress,

        @Size(max = 30)
        String companyPhone,

        @Size(max = 100)
        String registrationNumber,

        Instant subscriptionExpiresAt
) {
}
