package io.aygh.identity.dto.response;

import io.aygh.identity.entity.UserRole;
import io.aygh.identity.entity.UserStatus;
import io.aygh.shared.entity.Gender;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StaffResponse(
        UUID id,
        String username,
        String email,
        UserRole role,
        UserStatus status,
        Instant expiresAt,
        Instant lastLoginAt,
        String fullName,
        LocalDate dob,
        Gender gender,
        String country,
        String mobileNumber,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String zipCode,
        Instant createdAt,
        Instant updatedAt
) {
}
