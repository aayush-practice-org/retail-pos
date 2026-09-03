package io.aygh.purchase.dto.response;

import io.aygh.shared.entity.PaymentMethod;
import io.aygh.shared.entity.TaxScheme;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A purchase as it appears in a list: the bill, who it came from, and what it
 * cost — nothing that would need the lines loaded.
 */
public record PurchaseSummaryResponse(
        Long id,
        String billNumber,
        LocalDate purchaseDate,
        Long vendorId,
        String vendorName,
        PaymentMethod paymentMethod,
        TaxScheme taxScheme,
        BigDecimal subTotal,
        BigDecimal discountAmount,
        BigDecimal taxableAmount,
        BigDecimal vatAmount,
        BigDecimal netTotal,
        int itemCount,
        Instant createdAt
) {
}
