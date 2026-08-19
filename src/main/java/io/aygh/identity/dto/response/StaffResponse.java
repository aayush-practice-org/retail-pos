package io.aygh.identity.dto.response;

import java.time.Instant;
import java.util.UUID;

public record StaffResponse(
        UUID id,
        String username,
        String fullName,
        String email,
        String phone,
        String role,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}
