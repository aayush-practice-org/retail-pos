package io.aygh.inventory.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** How a product is bought: in which unit, how much that holds, at what cost. */
@Getter
@Setter
@NoArgsConstructor
public class ProductPurchaseUnitResponse extends BaseResponse {

    private UnitResponse unit;

    /** How many of the product's base unit one of these holds. */
    private BigDecimal packQuantity;
    private BigDecimal purchasePrice;
    // Named explicitly: Lombok gives a boolean field "isDefault" the accessors
    // isDefault()/setDefault(), which Jackson would otherwise read as a field
    // called "default" on the wire.
    @JsonProperty("isDefault")
    private boolean isDefault;
    private boolean active;

    /** The rate in force today, flattened. The history is on the detail view. */
    private BigDecimal currentVatRate;
}
