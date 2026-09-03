package io.aygh.sales.dto.request;

import io.aygh.shared.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Settling a bill that went out unpaid or part-paid. */
public record SalePaymentRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
        BigDecimal amount,

        PaymentMethod paymentMethod
) {
}
