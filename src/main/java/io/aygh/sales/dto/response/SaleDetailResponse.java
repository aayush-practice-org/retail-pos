package io.aygh.sales.dto.response;

import io.aygh.sales.entity.SaleChannel;
import io.aygh.shared.entity.PaymentMethod;
import io.aygh.shared.entity.PaymentStatus;
import io.aygh.shared.entity.TaxScheme;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** One bill in full — everything the invoice PDF needs bar the mart's own details. */
public record SaleDetailResponse(
        Long id,
        String invoiceNumber,
        Instant soldAt,
        SaleChannel channel,
        TaxScheme taxScheme,

        Long customerId,
        String customerName,
        String customerPhone,
        String customerPan,

        BigDecimal subTotal,
        BigDecimal discountAmount,
        BigDecimal taxableAmount,
        BigDecimal vatAmount,
        BigDecimal netTotal,

        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        BigDecimal paidAmount,
        BigDecimal changeAmount,
        BigDecimal dueAmount,

        String remark,
        List<SaleItemResponse> items
) {
}
