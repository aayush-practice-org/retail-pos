package io.aygh.inventory.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * One purchase unit with its full VAT history — every rate that has applied and
 * the window each applied over. Kept off the product view, where it would bury
 * the configuration under an audit trail nobody asked for.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductPurchaseUnitDetailResponse extends ProductPurchaseUnitResponse {

    private List<ProductVatResponse> vatRates = new ArrayList<>();
}
