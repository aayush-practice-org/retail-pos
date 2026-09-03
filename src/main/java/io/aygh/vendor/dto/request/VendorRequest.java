package io.aygh.vendor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The editable face of a vendor. Create and update take the same shape — there
 * is no field a vendor is born with and then may not change.
 */
public record VendorRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100)
        String name,

        @NotBlank(message = "Address is required")
        @Size(max = 255)
        String address,

        @Size(max = 30)
        String contactNumber,

        @Size(max = 30)
        String panNumber
) {
}
