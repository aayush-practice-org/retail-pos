package io.aygh.customer.dto.response;

import io.aygh.shared.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public record CustomerSettlementResponse(
        Long customerId,
        String customerName,
        BigDecimal amountSettled,
        BigDecimal previousBalance,
        BigDecimal remainingBalance,
        PaymentMethod paymentMethod,
        List<SettledInvoiceSummary> settledInvoices
) {
}
