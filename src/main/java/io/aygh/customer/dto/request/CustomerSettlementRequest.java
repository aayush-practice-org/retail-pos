package io.aygh.customer.dto.request;

import io.aygh.shared.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request to settle customer credit balance.
 */
public record CustomerSettlementRequest(

        @NotNull(message = "Settlement amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
        BigDecimal amount,

        PaymentMethod paymentMethod,

        @Size(max = 255, message = "Remark must not exceed 255 characters")
        String remark
) {
}
