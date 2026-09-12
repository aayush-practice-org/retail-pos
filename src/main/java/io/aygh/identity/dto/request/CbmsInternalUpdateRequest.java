package io.aygh.identity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CbmsInternalUpdateRequest(
        @NotBlank(message = "CBMS username is required")
        String cbmsUsername,

        @NotBlank(message = "CBMS password is required")
        String cbmsPassword,

        boolean taxIncluded
) {
}
