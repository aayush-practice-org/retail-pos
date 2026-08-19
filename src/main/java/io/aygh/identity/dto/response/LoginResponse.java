package io.aygh.identity.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record LoginResponse(
        UUID id,
        String token,
        String username,
        String fullName,
        String email,
        String role,
        Instant expiresAt
) {
}
