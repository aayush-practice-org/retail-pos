package io.aygh.inventory.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * The one-category view: the summary plus the unit policy products in it
 * inherit, split by which side of the trade each unit is permitted for.
 */
@Getter
@Setter
@NoArgsConstructor
public class CategoryDetailResponse extends CategorySummaryResponse {

    private List<UnitResponse> purchaseUnits = new ArrayList<>();
    private List<UnitResponse> sellingUnits = new ArrayList<>();
    private long productCount;
}
