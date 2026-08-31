package io.aygh.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** What stays editable about how a product is sold. */
@Getter
@Setter
@NoArgsConstructor
public abstract class ProductSellingUnitBaseRequest {

    @NotNull(message = "Pack quantity is required")
    @DecimalMin(value = "0.000001", message = "Pack quantity must be greater than zero")
    private BigDecimal packQuantity;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.00", message = "Selling price cannot be negative")
    private BigDecimal sellingPrice;

    /** Printed price, kept beside the real one so a discount is visible. */
    @DecimalMin(value = "0.00", message = "MRP cannot be negative")
    private BigDecimal mrp;

    @Size(max = 64)
    private String sku;

    @Size(max = 64)
    private String barcode;

    /** Setting this clears the flag on the product's other selling units. */
    // Named explicitly: Lombok gives a boolean field "isDefault" the accessors
    // isDefault()/setDefault(), which Jackson would otherwise read as a field
    // called "default" on the wire.
    @JsonProperty("isDefault")
    private boolean isDefault;

    private boolean active = true;
}
