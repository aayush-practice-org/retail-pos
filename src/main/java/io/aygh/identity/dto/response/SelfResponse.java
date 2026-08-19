package io.aygh.identity.dto.response;

import java.util.UUID;

public record SelfResponse(
        UUID id,
        String username,
        String fullName,
        String email,
        String phone,
        String role
) {
}
