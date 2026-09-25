package io.aygh.purchase.dto.response;

import io.aygh.sales.dto.response.PaymentTypeTotal;
import io.aygh.shared.response.ReturnTotals;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Purchases over a window, and what was sent back against them.
 * <p>
 * {@code netPurchases} and {@code vatAmount} are what was bought; the return
 * figures are the debit notes raised in the same window, and the "after returns"
 * pair is what the mart actually kept.
 */
@NoArgsConstructor
@Getter
@Setter
public class PurchaseReportSummary {
    private BigDecimal netPurchases;
    private BigDecimal vatAmount;
    private Long totalPurchasesCount;
    private List<PaymentTypeTotal> paymentTypeTotals;

    private Long purchaseReturnCount = 0L;
    private BigDecimal purchaseReturnAmount = BigDecimal.ZERO;
    private BigDecimal purchaseReturnVatAmount = BigDecimal.ZERO;
    private BigDecimal netPurchasesAfterReturns;
    private BigDecimal vatAmountAfterReturns;

    /** Built by the report query; the return figures are filled in after. */
    public PurchaseReportSummary(BigDecimal netPurchases, BigDecimal vatAmount, Long totalPurchasesCount,
                                 List<PaymentTypeTotal> paymentTypeTotals) {
        this.netPurchases = netPurchases == null ? BigDecimal.ZERO : netPurchases;
        this.vatAmount = vatAmount == null ? BigDecimal.ZERO : vatAmount;
        this.totalPurchasesCount = totalPurchasesCount == null ? 0L : totalPurchasesCount;
        this.paymentTypeTotals = paymentTypeTotals;
        this.netPurchasesAfterReturns = this.netPurchases;
        this.vatAmountAfterReturns = this.vatAmount;
    }

    public void applyReturns(ReturnTotals returns) {
        purchaseReturnCount = returns.count();
        purchaseReturnAmount = returns.netTotal();
        purchaseReturnVatAmount = returns.vatAmount();
        netPurchasesAfterReturns = netPurchases.subtract(returns.netTotal());
        vatAmountAfterReturns = vatAmount.subtract(returns.vatAmount());
    }
}
