package io.aygh.purchase.dto.response;

import io.aygh.shared.entity.PaymentMethod;
import io.aygh.shared.entity.TaxScheme;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** One purchase in full, lines included. */
public record PurchaseDetailResponse(
        Long id,
        String billNumber,
        LocalDate purchaseDate,
        Long vendorId,
        String vendorName,
        String vendorPanNumber,
        String vendorAddress,
        PaymentMethod paymentMethod,
        TaxScheme taxScheme,
        BigDecimal subTotal,
        BigDecimal discountAmount,
        BigDecimal taxableAmount,
        BigDecimal vatAmount,
        BigDecimal netTotal,
        String remark,
        List<PurchaseItemResponse> items,
        Instant createdAt
) {
}
