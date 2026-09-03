package io.aygh.vendor.dto.response;

import java.time.Instant;

/** A vendor as every screen that lists or opens one sees it. */
public record VendorResponse(
        Long id,
        String name,
        String address,
        String contactNumber,
        String panNumber,
        Instant createdAt,
        Instant updatedAt
) {
}
