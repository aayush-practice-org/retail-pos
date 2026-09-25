package io.aygh.sales.helper;

import io.aygh.sales.entity.Sale;
import io.aygh.sales.entity.SalesReturn;
import io.aygh.sales.entity.SalesReturnItem;
import io.aygh.shared.entity.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SalesReturnCalculatorTest {

    private final SalesReturnCalculator calculator = new SalesReturnCalculator();

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static Sale sale(String subTotal, String discount, String taxable, String vat, String paid) {
        BigDecimal net = bd(taxable).add(bd(vat));
        Sale sale = Sale.builder()
                .subTotal(bd(subTotal))
                .discountAmount(bd(discount))
                .taxableAmount(bd(taxable))
                .vatAmount(bd(vat))
                .netTotal(net)
                .build();
        sale.settle(bd(paid));
        return sale;
    }

    private static SalesReturn returnOf(String... lineTotals) {
        SalesReturn salesReturn = new SalesReturn();
        salesReturn.setItems(new ArrayList<>());
        for (String lineTotal : lineTotals) {
            salesReturn.addItem(SalesReturnItem.builder().lineTotal(bd(lineTotal)).build());
        }
        return salesReturn;
    }

    // ── Prorating ────────────────────────────────────────────────────────

    @Test
    void vatInclusiveReturnCreditsItsShareOfTaxableAndVat() {
        // 113 shelf price with VAT inside: 100 taxable + 13 VAT
        Sale sale = sale("113.00", "0", "100.00", "13.00", "113.00");
        SalesReturn salesReturn = returnOf("56.50");

        calculator.applyTotals(salesReturn, sale, List.of(), false);

        assertEquals(bd("50.00"), salesReturn.getTaxableAmount());
        assertEquals(bd("6.50"), salesReturn.getVatAmount());
        assertEquals(bd("56.50"), salesReturn.getNetTotal());
    }

    @Test
    void vatOnTopReturnCarriesItsShareOfTheBillDiscount() {
        // 200 basket, 20 off, VAT on top: 180 taxable + 23.40 VAT
        Sale sale = sale("200.00", "20.00", "180.00", "23.40", "203.40");
        SalesReturn salesReturn = returnOf("50.00");

        calculator.applyTotals(salesReturn, sale, List.of(), false);

        assertEquals(bd("5.00"), salesReturn.getDiscountAmount());
        assertEquals(bd("45.00"), salesReturn.getTaxableAmount());
        assertEquals(bd("5.85"), salesReturn.getVatAmount());
        assertEquals(bd("50.85"), salesReturn.getNetTotal());
    }

    @Test
    void panBillReturnCarriesNoVat() {
        Sale sale = sale("300.00", "0", "300.00", "0", "300.00");
        SalesReturn salesReturn = returnOf("100.00");

        calculator.applyTotals(salesReturn, sale, List.of(), false);

        assertEquals(bd("0.00"), salesReturn.getVatAmount());
        assertEquals(bd("100.00"), salesReturn.getNetTotal());
    }

    @Test
    void returnsInThirdsAddUpToTheBillExactly() {
        // 100 / 3 does not divide to the paisa; the last return takes what is left
        Sale sale = sale("100.00", "0", "88.50", "11.50", "100.00");

        SalesReturn first = returnOf("33.33");
        calculator.applyTotals(first, sale, List.of(), false);
        SalesReturn second = returnOf("33.33");
        calculator.applyTotals(second, sale, List.of(first), false);
        SalesReturn last = returnOf("33.34");
        calculator.applyTotals(last, sale, List.of(first, second), true);

        assertEquals(bd("88.50"), first.getTaxableAmount().add(second.getTaxableAmount()).add(last.getTaxableAmount()));
        assertEquals(bd("11.50"), first.getVatAmount().add(second.getVatAmount()).add(last.getVatAmount()));
        assertEquals(bd("100.00"), first.getNetTotal().add(second.getNetTotal()).add(last.getNetTotal()));
    }

    // ── Crediting the bill ───────────────────────────────────────────────

    @Test
    void cashBillReturnIsRefunded() {
        Sale sale = sale("100.00", "0", "100.00", "0", "100.00");

        BigDecimal refund = sale.applyReturn(bd("30.00"));

        assertEquals(bd("30.00"), refund);
        assertEquals(bd("70.00"), sale.getPaidAmount());
        assertEquals(0, sale.dueAmount().signum());
        assertEquals(PaymentStatus.PAID, sale.getPaymentStatus());
    }

    @Test
    void creditBillReturnComesOffWhatIsOwed() {
        Sale sale = sale("100.00", "0", "100.00", "0", "0");

        BigDecimal refund = sale.applyReturn(bd("30.00"));

        assertEquals(0, refund.signum());
        assertEquals(bd("70.00"), sale.dueAmount());
        assertEquals(PaymentStatus.UNPAID, sale.getPaymentStatus());
    }

    @Test
    void partlyPaidBillRefundsOnlyWhatWasOverpaid() {
        Sale sale = sale("100.00", "0", "100.00", "0", "50.00");

        BigDecimal refund = sale.applyReturn(bd("80.00"));

        // Owes 20 now, had paid 50: 30 back, and the bill is settled
        assertEquals(bd("30.00"), refund);
        assertEquals(bd("20.00"), sale.getPaidAmount());
        assertEquals(PaymentStatus.PAID, sale.getPaymentStatus());
    }
}
