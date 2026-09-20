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

    /**
     * Which product this prices. Nested under a product for most of its
     * callers and therefore redundant there — but a barcode scan arrives at
     * this row with nothing else, and a sale line is keyed by product, so
     * without these the till would have to look the product up again to ring
     * up what it just scanned.
     */
    private Long productId;
    private String productName;

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
