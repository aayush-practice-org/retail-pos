package io.aygh.sales.dto.response;

import io.aygh.shared.entity.TaxScheme;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** A credit note in full. Items are empty on listings. */
public record SalesReturnResponse(
        Long id,
        String creditNoteNumber,
        Long saleId,
        String invoiceNumber,
        Instant returnedAt,
        String reason,
        TaxScheme taxScheme,
        String customerName,
        String customerPan,
        String nepaliDate,
        String fiscalYear,

        BigDecimal subTotal,
        BigDecimal discountAmount,
        BigDecimal taxableAmount,
        BigDecimal vatAmount,
        BigDecimal netTotal,
        /** Cash handed back; the rest of netTotal came off what was owed. */
        BigDecimal refundAmount,

        Boolean syncWithIrd,
        String remark,
        List<SalesReturnItemResponse> items
) {
}
