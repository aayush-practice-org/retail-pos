package io.aygh.sales.dto.response;

import io.aygh.sales.entity.SaleChannel;
import io.aygh.shared.entity.PaymentMethod;
import io.aygh.shared.entity.PaymentStatus;
import io.aygh.shared.entity.TaxScheme;

import java.math.BigDecimal;
import java.time.Instant;

/** A bill as it appears in a list — nothing that would need the lines loaded. */
public record SaleSummaryResponse(
        Long id,
        String invoiceNumber,
        Instant soldAt,
        SaleChannel channel,
        TaxScheme taxScheme,
        String customerName,
        BigDecimal subTotal,
        BigDecimal discountAmount,
        BigDecimal vatAmount,
        BigDecimal netTotal,
        BigDecimal paidAmount,
        BigDecimal dueAmount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        int itemCount
) {
}
