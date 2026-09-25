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

        /** BS date entered at the till, e.g. "2081.09.05". May be null for old records. */
        String nepaliDate,
        /** IRD fiscal year, e.g. "2081.082". May be null for old records. */
        String fiscalYear,

        BigDecimal subTotal,
        BigDecimal discountAmount,
        BigDecimal taxableAmount,
        BigDecimal vatAmount,
        BigDecimal netTotal,
        /** Credited back on sales returns. */
        BigDecimal returnedAmount,

        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        BigDecimal paidAmount,
        BigDecimal changeAmount,
        BigDecimal dueAmount,

        String remark,
        Integer printCount,
        Boolean isBillPrinted,
        /** Whether the IRD's CBMS accepted this bill. */
        Boolean syncWithIrd,
        List<SaleItemResponse> items
) {
}
