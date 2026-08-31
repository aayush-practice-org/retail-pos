package io.aygh.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * What stays editable about how a product is bought.
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class ProductPurchaseUnitBaseRequest {

    /**
     * How many of the product's base unit one of these holds — a 50kg sack of a
     * product counted in grams is 50000.
     */
    @NotNull(message = "Pack quantity is required")
    @DecimalMin(value = "0.000001", message = "Pack quantity must be greater than zero")
    private BigDecimal packQuantity;

    @DecimalMin(value = "0.00", message = "Purchase price cannot be negative")
    private BigDecimal purchasePrice;

    /**
     * Setting this clears the flag on the product's other purchase units.
     */
    // Named explicitly: Lombok gives a boolean field "isDefault" the accessors
    // isDefault()/setDefault(), which Jackson would otherwise read as a field
    // called "default" on the wire.
    @JsonProperty("isDefault")
    private boolean isDefault;

    private boolean active = true;
}
