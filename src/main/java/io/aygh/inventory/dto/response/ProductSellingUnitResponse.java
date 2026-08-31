package io.aygh.inventory.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** How a product is sold: in which unit, how much that holds, at what price. */
@Getter
@Setter
@NoArgsConstructor
public class ProductSellingUnitResponse extends BaseResponse {

    private UnitResponse unit;

    private BigDecimal packQuantity;
    private BigDecimal sellingPrice;
    private BigDecimal mrp;
    private String sku;
    private String barcode;
    // Named explicitly: Lombok gives a boolean field "isDefault" the accessors
    // isDefault()/setDefault(), which Jackson would otherwise read as a field
    // called "default" on the wire.
    @JsonProperty("isDefault")
    private boolean isDefault;
    private boolean active;
}
