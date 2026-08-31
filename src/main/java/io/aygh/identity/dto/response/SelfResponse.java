package io.aygh.identity.dto.response;

import io.aygh.identity.entity.UserRole;
import io.aygh.identity.entity.UserStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record SelfResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        String mobileNumber,
        UserRole role,
        UserStatus status,

        UUID tenantId,
        String tenantSlug,
        String companyName,

        Instant expiresAt,
        Instant lastLoginAt
) {
}
