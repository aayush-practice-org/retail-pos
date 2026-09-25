package io.aygh.purchase.helper;

import io.aygh.purchase.entity.Purchase;
import io.aygh.purchase.entity.PurchaseReturn;
import io.aygh.purchase.entity.PurchaseReturnItem;
import io.aygh.shared.entity.TaxScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

/**
 * What a debit note comes to.
 * <p>
 * The returned lines carry their share of the bill's discount, and VAT is taken
 * per line at that line's rate, the way {@link PurchaseCalculator} charged it:
 * a bill mixes goods at 13% and goods at none, so one flat share of the bill's
 * VAT would debit the vendor for tax that was never paid on the goods returned.
 * <p>
 * Each return is capped at what is left of the bill, and the one that sends back
 * the last of it takes exactly that — the debit notes on a bill add up to it.
 */
@Component
@RequiredArgsConstructor
public class PurchaseReturnCalculator {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PurchaseCalculator purchaseCalculator;

    public void applyTotals(PurchaseReturn purchaseReturn, Purchase purchase,
                            List<PurchaseReturn> previous, boolean completesPurchase) {
        BigDecimal subTotal = purchaseReturn.getItems().stream()
                .map(PurchaseReturnItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal remainingDiscount = remaining(purchase.getDiscountAmount(), previous, PurchaseReturn::getDiscountAmount);
        BigDecimal remainingTaxable = remaining(purchase.getTaxableAmount(), previous, PurchaseReturn::getTaxableAmount);
        BigDecimal remainingVat = remaining(purchase.getVatAmount(), previous, PurchaseReturn::getVatAmount);

        BigDecimal discount;
        BigDecimal taxable;
        BigDecimal vat;

        if (completesPurchase) {
            discount = remainingDiscount;
            taxable = remainingTaxable;
            vat = remainingVat;
        } else if (purchase.getSubTotal().signum() <= 0) {
            discount = BigDecimal.ZERO;
            taxable = BigDecimal.ZERO;
            vat = BigDecimal.ZERO;
        } else {
            // What of each rupee on the bill was left after its discount
            BigDecimal kept = purchase.getTaxableAmount().divide(purchase.getSubTotal(), MathContext.DECIMAL64);

            discount = scale(subTotal.multiply(BigDecimal.ONE.subtract(kept))).min(remainingDiscount);
            taxable = subTotal.subtract(discount).min(remainingTaxable);
            vat = purchase.getTaxScheme() == TaxScheme.VAT
                    ? scale(vatOn(purchaseReturn.getItems(), kept)).min(remainingVat)
                    : BigDecimal.ZERO.setScale(MONEY_SCALE);
        }

        purchaseReturn.setSubTotal(subTotal);
        purchaseReturn.setDiscountAmount(discount);
        purchaseReturn.setTaxableAmount(taxable);
        purchaseReturn.setVatAmount(vat);
        purchaseReturn.setNetTotal(taxable.add(vat));
    }

    private BigDecimal vatOn(List<PurchaseReturnItem> items, BigDecimal kept) {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseReturnItem item : items) {
            BigDecimal rate = purchaseCalculator.vatRateFor(item.getPurchaseItem());
            if (rate.signum() > 0) {
                total = total.add(item.getLineTotal().multiply(kept)
                        .multiply(rate).divide(HUNDRED, MathContext.DECIMAL64));
            }
        }
        return total;
    }

    private static BigDecimal remaining(BigDecimal onBill, List<PurchaseReturn> previous,
                                        Function<PurchaseReturn, BigDecimal> amount) {
        BigDecimal returned = previous.stream()
                .map(amount)
                .map(PurchaseReturnCalculator::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return scale(zeroIfNull(onBill).subtract(returned).max(BigDecimal.ZERO));
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
