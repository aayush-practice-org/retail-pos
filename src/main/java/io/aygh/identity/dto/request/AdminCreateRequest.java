package io.aygh.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AdminCreateRequest(

        // ── The admin's own account ───────────────────────────────────────
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @Size(max = 255)
        String fullName,

        @Size(max = 30)
        String mobileNumber,

        // ── The company ───────────────────────────────────────────────────
        @NotBlank(message = "Company name is required")
        @Size(max = 255)
        String companyName,

        @Size(max = 255)
        String companyAddress,

        @Size(max = 30)
        String companyPhone,

        @Size(max = 100)
        String registrationNumber,

        /**
         * The schema this mart's data will live in. Optional: left out, it is
         * derived from the company name. Immutable once the mart exists.
         */
        @Pattern(regexp = "^[a-z][a-z0-9_]{1,62}$",
                message = "Slug must be 2 to 63 lowercase letters, digits or underscores, starting with a letter")
        String slug,

        /** When the mart's subscription runs out; null for an open-ended one. */
        Instant subscriptionExpiresAt
) {
}
