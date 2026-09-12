package io.aygh.customer.dto.response;

import io.aygh.shared.entity.PaymentStatus;

import java.math.BigDecimal;

public record SettledInvoiceSummary(
        Long saleId,
        String invoiceNumber,
        BigDecimal invoiceNetTotal,
        BigDecimal previouslyPaid,
        BigDecimal amountApplied,
        BigDecimal remainingDue,
        PaymentStatus paymentStatus
) {
}
