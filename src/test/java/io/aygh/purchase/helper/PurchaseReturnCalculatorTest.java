package io.aygh.purchase.helper;

import io.aygh.purchase.entity.Purchase;
import io.aygh.purchase.entity.PurchaseItem;
import io.aygh.purchase.entity.PurchaseReturn;
import io.aygh.purchase.entity.PurchaseReturnItem;
import io.aygh.shared.entity.TaxScheme;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PurchaseReturnCalculatorTest {

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    // Soap at 13% VAT, rice at none — one bill mixes both
    private final PurchaseItem soap = PurchaseItem.builder().id(1L).lineTotal(bd("1000.00")).build();
    private final PurchaseItem rice = PurchaseItem.builder().id(2L).lineTotal(bd("500.00")).build();

    private final PurchaseReturnCalculator calculator = new PurchaseReturnCalculator(
            new PurchaseCalculator(null) {
                @Override
                public BigDecimal vatRateFor(PurchaseItem item) {
                    return Map.of(1L, bd("13"), 2L, BigDecimal.ZERO).get(item.getId());
                }
            });

    /** 1500 of goods, 150 off (10%), VAT 13% on the soap only: 900 × 13% = 117. */
    private Purchase purchase() {
        return Purchase.builder()
                .taxScheme(TaxScheme.VAT)
                .subTotal(bd("1500.00"))
                .discountAmount(bd("150.00"))
                .taxableAmount(bd("1350.00"))
                .vatAmount(bd("117.00"))
                .netTotal(bd("1467.00"))
                .build();
    }

    private static PurchaseReturn returnOf(PurchaseItem line, String lineTotal) {
        PurchaseReturn purchaseReturn = new PurchaseReturn();
        purchaseReturn.setItems(new ArrayList<>());
        purchaseReturn.addItem(PurchaseReturnItem.builder().purchaseItem(line).lineTotal(bd(lineTotal)).build());
        return purchaseReturn;
    }

    @Test
    void damagedTaxedGoodsCarryTheirOwnVat() {
        PurchaseReturn purchaseReturn = returnOf(soap, "500.00");

        calculator.applyTotals(purchaseReturn, purchase(), List.of(), false);

        assertEquals(bd("50.00"), purchaseReturn.getDiscountAmount());
        assertEquals(bd("450.00"), purchaseReturn.getTaxableAmount());
        assertEquals(bd("58.50"), purchaseReturn.getVatAmount());
        assertEquals(bd("508.50"), purchaseReturn.getNetTotal());
    }

    @Test
    void untaxedGoodsCarryNoVatEvenOnAVatBill() {
        PurchaseReturn purchaseReturn = returnOf(rice, "100.00");

        calculator.applyTotals(purchaseReturn, purchase(), List.of(), false);

        assertEquals(bd("90.00"), purchaseReturn.getTaxableAmount());
        assertEquals(bd("0.00"), purchaseReturn.getVatAmount());
    }

    @Test
    void lastReturnTakesExactlyWhatIsLeftOfTheBill() {
        Purchase purchase = purchase();
        PurchaseReturn first = returnOf(soap, "500.00");
        calculator.applyTotals(first, purchase, List.of(), false);

        PurchaseReturn rest = new PurchaseReturn();
        rest.setItems(new ArrayList<>());
        rest.addItem(PurchaseReturnItem.builder().purchaseItem(soap).lineTotal(bd("500.00")).build());
        rest.addItem(PurchaseReturnItem.builder().purchaseItem(rice).lineTotal(bd("500.00")).build());
        calculator.applyTotals(rest, purchase, List.of(first), true);

        assertEquals(bd("1467.00"), first.getNetTotal().add(rest.getNetTotal()));
        assertEquals(bd("117.00"), first.getVatAmount().add(rest.getVatAmount()));
    }
}
