package io.aygh.identity.dto.response;

import io.aygh.identity.entity.TaxRegistration;

import java.util.UUID;

public record CbmsInternalResponse(
        Long id,
        UUID tenantId,
        String tenantSlug,
        String cbmsUsername,
        String cbmsPassword,
        TaxRegistration taxRegistration,
        boolean taxIncluded,
        String pan
) {
}
