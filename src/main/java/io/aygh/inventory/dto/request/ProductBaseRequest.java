package io.aygh.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The fields of a product that stay editable for its whole life.
 * <p>
 * Its subclasses are where the difference lives: creation also fixes the
 * category and the base unit, and neither may be changed afterwards. Expressing
 * that as two subclasses rather than one nullable request means the update
 * endpoint has no field for them at all — there is nothing for a caller to send
 * and nothing for the service to have to ignore.
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class ProductBaseRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 64)
    private String productCode;

    @Size(max = 64)
    private String baseCode;

    @Size(max = 1000)
    private String description;

    @Size(max = 255)
    private String brand;

    @Size(max = 255)
    private String image;
}
