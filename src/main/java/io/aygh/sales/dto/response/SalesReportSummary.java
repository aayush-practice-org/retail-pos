package io.aygh.sales.dto.response;

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
public class SalesReportSummary {
    private BigDecimal netSales;
    private  BigDecimal vatAmount;
    private  Long totalSalesCount;
    private  List<PaymentTypeTotal> paymentTypeTotals;
}