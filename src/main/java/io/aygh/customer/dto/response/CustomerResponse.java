package io.aygh.customer.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record CustomerResponse(
        Long id,
        String name,
        String phone,
        String email,
        String panNumber,
        String address,
        BigDecimal creditLimit,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
