package io.aygh.inventory.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One VAT rate. */
@Getter
@Setter
@NoArgsConstructor
public class ProductVatResponse extends BaseResponse {

    /** A percentage: 13 means 13%. */
    private BigDecimal rate;
}

