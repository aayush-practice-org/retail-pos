package io.aygh.purchase.helper;

import io.aygh.inventory.entity.ProductPurchaseVat;
import io.aygh.inventory.repository.ProductPurchaseVatRepository;
import io.aygh.purchase.entity.Purchase;
import io.aygh.purchase.entity.PurchaseItem;
import io.aygh.shared.entity.TaxScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * What a purchase costs, worked out from its lines.
 * <p>
 * VAT is taken per line at the rate that was in force for that product's
 * purchase unit on the purchase date, not at one flat rate for the bill: a mart
 * buys goods at different rates, and some at none. That is what
 * {@link ProductPurchaseVat} is effective-dated for.
 * <p>
 * A header discount is apportioned across the lines in proportion to their
 * totals before VAT is taken. Charging VAT on the undiscounted line and then
 * subtracting the discount would over-collect on the goods that were discounted.
 */
@Component
@RequiredArgsConstructor
public class PurchaseCalculator {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ProductPurchaseVatRepository vatRepository;

    public void applyTotals(Purchase purchase, List<PurchaseItem> items) {
        BigDecimal subTotal = items.stream()
                .map(PurchaseItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal discount = purchase.getDiscountAmount() == null
                ? BigDecimal.ZERO
                : purchase.getDiscountAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal taxable = subTotal.subtract(discount);
        BigDecimal vat = purchase.getTaxScheme() == TaxScheme.VAT
                ? vatOn(items, subTotal, taxable, purchase.getPurchaseDate())
                : BigDecimal.ZERO;

        purchase.setSubTotal(subTotal);
        purchase.setDiscountAmount(discount);
        purchase.setTaxableAmount(taxable);
        purchase.setVatAmount(vat);
        purchase.setNetTotal(taxable.add(vat));
    }

    /**
     * The discount ratio is the same for every line, so each line's taxable share
     * is its own total scaled by it. A sub total of zero cannot be apportioned —
     * and carries no VAT either.
     */
    private BigDecimal vatOn(List<PurchaseItem> items, BigDecimal subTotal,
                             BigDecimal taxable, LocalDate on) {
        if (subTotal.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal ratio = taxable.divide(subTotal, MathContext.DECIMAL64);
        BigDecimal total = BigDecimal.ZERO;

        for (PurchaseItem item : items) {
            BigDecimal rate = vatRateFor(item);
            if (rate.signum() <= 0) {
                continue;
            }
            BigDecimal lineTaxable = item.getLineTotal().multiply(ratio);
            total = total.add(lineTaxable.multiply(rate).divide(HUNDRED, MathContext.DECIMAL64));
        }

        return total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * The VAT rate, in percent, a line is charged at. Zero when the purchase unit
     * has no rate — not every good is taxed.
     */
    public BigDecimal vatRateFor(PurchaseItem item) {
        return vatRepository.findTopByProductPurchaseUnitIdOrderByCreatedAtDesc(item.getPurchaseUnit().getId())
                .map(ProductPurchaseVat::getRate)
                .orElse(BigDecimal.ZERO);
    }
}
