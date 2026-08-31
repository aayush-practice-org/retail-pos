package io.aygh.identity.dto.response;

import io.aygh.identity.entity.ProvisioningStatus;
import io.aygh.identity.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * A mart as the super admin's screens see it: the company, the account that owns
 * it, and whether its schema is actually ready to be used.
 */
public record AdminResponse(
        UUID id,

        String username,
        String email,
        String fullName,
        String mobileNumber,
        UserStatus status,

        String companyName,
        String companyAddress,
        String companyPhone,
        String registrationNumber,
        String slug,

        Instant subscriptionExpiresAt,

        ProvisioningStatus provisioningStatus,
        String provisioningError,
        Instant provisionedAt,

        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
}
