package io.aygh.sales.dto.response;

import io.aygh.shared.response.ReturnTotals;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Sales over a window, and what came back against them.
 * <p>
 * {@code netSales} and {@code vatAmount} are what was billed; the return figures
 * are the credit notes raised in the same window, and the "after returns" pair
 * is what the mart actually kept.
 */
@NoArgsConstructor
@Getter
@Setter
public class SalesReportSummary {
    private BigDecimal netSales;
    private BigDecimal vatAmount;
    private Long totalSalesCount;
    private List<PaymentTypeTotal> paymentTypeTotals;

    private Long salesReturnCount = 0L;
    private BigDecimal salesReturnAmount = BigDecimal.ZERO;
    private BigDecimal salesReturnVatAmount = BigDecimal.ZERO;
    private BigDecimal netSalesAfterReturns;
    private BigDecimal vatAmountAfterReturns;

    /** Built by the report query; the return figures are filled in after. */
    public SalesReportSummary(BigDecimal netSales, BigDecimal vatAmount, Long totalSalesCount,
                              List<PaymentTypeTotal> paymentTypeTotals) {
        this.netSales = netSales == null ? BigDecimal.ZERO : netSales;
        this.vatAmount = vatAmount == null ? BigDecimal.ZERO : vatAmount;
        this.totalSalesCount = totalSalesCount == null ? 0L : totalSalesCount;
        this.paymentTypeTotals = paymentTypeTotals;
        this.netSalesAfterReturns = this.netSales;
        this.vatAmountAfterReturns = this.vatAmount;
    }

    public void applyReturns(ReturnTotals returns) {
        salesReturnCount = returns.count();
        salesReturnAmount = returns.netTotal();
        salesReturnVatAmount = returns.vatAmount();
        netSalesAfterReturns = netSales.subtract(returns.netTotal());
        vatAmountAfterReturns = vatAmount.subtract(returns.vatAmount());
    }
}
