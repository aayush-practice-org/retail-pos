package io.aygh.identity.dto.request;

import io.aygh.identity.entity.TaxRegistration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CbmsInternalUpdateRequest(
        @NotBlank(message = "CBMS username is required")
        String cbmsUsername,

        @NotBlank(message = "CBMS password is required")
        String cbmsPassword,

        @NotNull(message = "Tax registration is required")
        TaxRegistration taxRegistration,

        boolean taxIncluded
) {
}
