package io.aygh.sales.helper;

import io.aygh.config.properties.TaxProperties;
import io.aygh.identity.entity.CbmsInternalEntity;
import io.aygh.identity.entity.TaxRegistration;
import io.aygh.identity.service.CbmsInternalProvider;
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
 * VAT behaviour is driven entirely by the tenant's CBMS configuration:
 * <ul>
 *   <li>PAN-registered (the default) → no VAT (NON_VAT scheme)</li>
 *   <li>VAT-registered + tax included  → extract VAT from price (price / 1.13)</li>
 *   <li>VAT-registered + tax excluded  → add VAT on top (price x 1.13)</li>
 * </ul>
 * The frontend never sends a taxScheme; the scheme is set on the Sale
 * entity here so downstream code (sales book, invoice PDF) can still read it.
 */
@Component
@RequiredArgsConstructor
public class SaleCalculator {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal VAT_DIVISOR = new BigDecimal("1.13");

    private final TaxProperties taxProperties;
    private final CbmsInternalProvider cbmsInternalProvider;

    public void applyTotals(Sale sale, List<SaleItem> items) {
        BigDecimal subTotal = items.stream()
                .map(SaleItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal discount = sale.getDiscountAmount() == null
                ? BigDecimal.ZERO
                : sale.getDiscountAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal gross = subTotal.subtract(discount);

        // Derive VAT behaviour from CBMS configuration
        CbmsInternalEntity cbms = cbmsInternalProvider.getOrCreate();
        boolean vatRegistered = cbms.getTaxRegistration() == TaxRegistration.VAT_REGISTERED;

        BigDecimal taxable;
        BigDecimal vat;
        TaxScheme scheme;

        if (!vatRegistered) {
            // PAN-registered — no VAT at all
            taxable = gross;
            vat = BigDecimal.ZERO;
            scheme = TaxScheme.NON_VAT;
        } else if (cbms.isTaxIncluded()) {
            // Prices already carry VAT: extract it (e.g. 113 net -> 100 taxable + 13 VAT)
            taxable = excludeVat(gross);
            vat = gross.subtract(taxable).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            scheme = TaxScheme.VAT;
        } else {
            // VAT added on top of shelf price (e.g. 100 taxable + 13 VAT -> 113 net)
            taxable = gross;
            vat = gross.multiply(taxProperties.vatRate())
                    .divide(HUNDRED, MathContext.DECIMAL64)
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            scheme = TaxScheme.VAT;
        }

        sale.setSubTotal(subTotal);
        sale.setDiscountAmount(discount);
        sale.setTaxableAmount(taxable);
        sale.setVatAmount(vat);
        sale.setNetTotal(taxable.add(vat));
        sale.setTaxScheme(scheme);
    }

    /**
     * What a price that already carries VAT is worth before it — the figure a tax
     * invoice prints, since every line above the VAT row is exclusive of VAT.
     */
    public static BigDecimal excludeVat(BigDecimal gross) {
        return (gross == null ? BigDecimal.ZERO : gross)
                .divide(VAT_DIVISOR, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** The rate a VAT bill raised now is charged at, for printing on the invoice. */
    public BigDecimal vatRate() {
        return taxProperties.vatRate();
    }
}
