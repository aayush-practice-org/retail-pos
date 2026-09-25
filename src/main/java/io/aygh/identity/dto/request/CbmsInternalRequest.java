package io.aygh.identity.dto.request;

import io.aygh.identity.entity.TaxRegistration;
import jakarta.validation.constraints.NotBlank;

public record CbmsInternalRequest(
        @NotBlank(message = "CBMS username is required")
        String cbmsUsername,

        @NotBlank(message = "CBMS password is required")
        String cbmsPassword,

        /** Optional: left out, the mart is registered as PAN only and charges no VAT. */
        TaxRegistration taxRegistration,

        /** true -> prices carry VAT (inclusive), false -> VAT added on top (exclusive) */
        boolean taxIncluded
) {
}
