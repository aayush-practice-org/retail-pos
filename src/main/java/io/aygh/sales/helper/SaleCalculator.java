package io.aygh.sales.helper;

import io.aygh.config.properties.TaxProperties;
import io.aygh.sales.entity.Sale;
import io.aygh.sales.entity.SaleItem;
import io.aygh.shared.entity.TaxScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * What a bill comes to.
 * <p>
 * Prices on the shelf are what the customer pays, so VAT is added <em>on top</em>
 * of the taxable amount rather than extracted from it. A mart that prints
 * VAT-inclusive shelf labels wants the other convention, and that is a decision
 * about the labels — worth stating here so the two are never mixed by accident.
 */
@Component
@RequiredArgsConstructor
public class SaleCalculator {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final TaxProperties taxProperties;

    public void applyTotals(Sale sale, List<SaleItem> items) {
        BigDecimal subTotal = items.stream()
                .map(SaleItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal discount = sale.getDiscountAmount() == null
                ? BigDecimal.ZERO
                : sale.getDiscountAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal taxable = subTotal.subtract(discount);
        BigDecimal vat = sale.getTaxScheme() == TaxScheme.VAT
                ? taxable.multiply(taxProperties.vatRate())
                        .divide(HUNDRED, MathContext.DECIMAL64)
                        .setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        sale.setSubTotal(subTotal);
        sale.setDiscountAmount(discount);
        sale.setTaxableAmount(taxable);
        sale.setVatAmount(vat);
        sale.setNetTotal(taxable.add(vat));
    }

    /** The rate a VAT bill raised now is charged at, for printing on the invoice. */
    public BigDecimal vatRate() {
        return taxProperties.vatRate();
    }
}
