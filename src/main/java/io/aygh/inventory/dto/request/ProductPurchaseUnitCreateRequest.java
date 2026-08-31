package io.aygh.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Adds the unit itself, which is fixed once created: repointing a configuration
 * at a different unit would leave its pack quantity describing the wrong thing.
 * Retire the row and add another instead.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductPurchaseUnitCreateRequest extends ProductPurchaseUnitBaseRequest {

    @NotNull(message = "Unit is required")
    private Long unitId;

    /** Optional opening VAT rate, so a purchase unit can be usable immediately. */
    private ProductVatRequest vat;
}
