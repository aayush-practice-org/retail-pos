package io.aygh.customer.dto.response;

import io.aygh.sales.dto.response.SaleSummaryResponse;

import java.math.BigDecimal;
import java.util.List;

public record CustomerOutstandingResponse(
        Long customerId,
        String customerName,
        String customerPhone,
        BigDecimal creditLimit,
        BigDecimal totalOutstanding,
        BigDecimal availableCredit,
        int unpaidInvoiceCount,
        List<SaleSummaryResponse> unpaidSales
) {
}
