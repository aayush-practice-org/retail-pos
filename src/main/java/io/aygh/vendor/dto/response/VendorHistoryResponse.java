package io.aygh.vendor.dto.response;

import java.time.Instant;

/**
 * One purchase in a vendor's trail. The vendor's name is carried alongside its
 * id so a history listing renders without a lookup per row.
 */
public record VendorHistoryResponse(
        Long id,
        Long vendorId,
        String vendorName,
        Long purchaseId,
        Instant createdAt,
        Instant updatedAt
) {
}
