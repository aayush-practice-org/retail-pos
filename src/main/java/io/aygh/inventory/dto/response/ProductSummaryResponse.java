package io.aygh.inventory.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A product as it appears in a list. The category is flattened to its name and
 * the price to the default selling unit's, so a page of rows needs no nested
 * objects and no extra queries.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductSummaryResponse extends BaseResponse {

    private String name;
    private String productCode;
    private String brand;
    private String image;
    private boolean active;

    private Long categoryId;
    private String categoryName;

    /** From the default selling unit, or null when none is marked default. */
    private BigDecimal sellingPrice;
    private String sellingUnitSymbol;
}
