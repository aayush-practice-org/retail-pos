package io.aygh.inventory.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * The full product: everything the list shows, plus the long text and the
 * trading configuration. Only the single-product endpoint returns this — the
 * list would otherwise carry two collections per row.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductDetailResponse extends ProductSummaryResponse {

    private String description;
    private String baseCode;

    /** The unit all stock for this product is held in. */
    private UnitResponse baseUnit;

    private List<ProductPurchaseUnitResponse> purchaseUnits = new ArrayList<>();
    private List<ProductSellingUnitResponse> sellingUnits = new ArrayList<>();
}
