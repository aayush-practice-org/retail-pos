package io.aygh.identity.dto.request;

import io.aygh.identity.entity.TaxRegistration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CbmsInternalRequest(
        @NotBlank(message = "CBMS username is required")
        String cbmsUsername,

        @NotBlank(message = "CBMS password is required")
        String cbmsPassword,

        @NotNull(message = "Tax registration is required")
        TaxRegistration taxRegistration,

        /** true -> prices carry VAT (inclusive), false -> VAT added on top (exclusive) */
        boolean taxIncluded
) {
}
