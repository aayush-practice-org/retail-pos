package io.aygh.identity.dto.response;

import io.aygh.identity.entity.UserRole;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;


@Builder
public record LoginResponse(
        UUID userId,
        String username,
        String email,
        String fullName,
        UserRole role,

        UUID tenantId,
        String tenantSlug,
        String companyName,

        String token,
        /** When the bearer token stops being accepted. */
        Instant tokenExpiresAt,
        /** When the account itself stops working — a subscription or contract end. */
        Instant accountExpiresAt
) {
}
