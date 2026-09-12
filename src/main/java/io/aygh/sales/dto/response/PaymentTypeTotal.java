package io.aygh.sales.dto.response;

import io.aygh.shared.entity.PaymentMethod;

import java.math.BigDecimal;

public record PaymentTypeTotal(
        PaymentMethod paymentMethod,
        BigDecimal netTotal
) {

}
