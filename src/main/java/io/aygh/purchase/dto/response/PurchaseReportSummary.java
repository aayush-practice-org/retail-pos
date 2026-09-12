package io.aygh.purchase.dto.response;

import io.aygh.sales.dto.response.PaymentTypeTotal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PurchaseReportSummary {
    private BigDecimal netPurchases;
    private BigDecimal vatAmount;
    private Long totalPurchasesCount;
    private List<PaymentTypeTotal> paymentTypeTotals;
}
