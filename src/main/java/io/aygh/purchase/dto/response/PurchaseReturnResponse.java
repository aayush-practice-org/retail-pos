package io.aygh.purchase.dto.response;

import io.aygh.shared.entity.TaxScheme;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** A debit note in full. Items are empty on listings. */
public record PurchaseReturnResponse(
        Long id,
        String debitNoteNumber,
        Long purchaseId,
        String billNumber,
        Long vendorId,
        String vendorName,
        LocalDate returnDate,
        String reason,
        TaxScheme taxScheme,

        BigDecimal subTotal,
        BigDecimal discountAmount,
        BigDecimal taxableAmount,
        BigDecimal vatAmount,
        BigDecimal netTotal,

        String remark,
        List<PurchaseReturnItemResponse> items,
        Instant createdAt
) {
}
