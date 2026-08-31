package io.aygh.inventory.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One VAT rate and the window it applied over. */
@Getter
@Setter
@NoArgsConstructor
public class ProductVatResponse extends BaseResponse {

    /** A percentage: 13 means 13%. */
    private BigDecimal rate;
    private LocalDate effectiveFrom;

    /** Null while this is the rate in force. */
    private LocalDate effectiveTo;
}
