package io.aygh.sales.helper;

import io.aygh.sales.entity.Sale;
import io.aygh.sales.entity.SalesReturn;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

/**
 * What a credit note comes to.
 * <p>
 * Nothing is priced afresh: the returned lines' share of the bill's sub total
 * is the share of its discount, taxable amount and VAT that is credited back.
 * That keeps the VAT treatment the bill was raised under — PAN, VAT on top, or
 * VAT inside the price — without having to know which it was.
 * <p>
 * Prorating rounds each return to the paisa on its own, so the return that
 * hands back the last of the bill takes whatever is left instead. However the
 * goods come back, the credit notes on a bill add up to the bill exactly.
 */
@Component
public class SalesReturnCalculator {

    private static final int MONEY_SCALE = 2;

    /**
     * @param salesReturn  carries its lines; sub total and amounts are set here
     * @param previous     returns already raised against the same sale
     * @param completesSale whether this return hands back everything left on the bill
     */
    public void applyTotals(SalesReturn salesReturn, Sale sale, List<SalesReturn> previous, boolean completesSale) {
        BigDecimal subTotal = salesReturn.getItems().stream()
                .map(item -> item.getLineTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal remainingDiscount = remaining(sale.getDiscountAmount(), previous, SalesReturn::getDiscountAmount);
        BigDecimal remainingTaxable = remaining(sale.getTaxableAmount(), previous, SalesReturn::getTaxableAmount);
        BigDecimal remainingVat = remaining(sale.getVatAmount(), previous, SalesReturn::getVatAmount);

        BigDecimal discount;
        BigDecimal taxable;
        BigDecimal vat;

        if (completesSale) {
            discount = remainingDiscount;
            taxable = remainingTaxable;
            vat = remainingVat;
        } else {
            BigDecimal share = sale.getSubTotal().signum() == 0
                    ? BigDecimal.ZERO
                    : subTotal.divide(sale.getSubTotal(), MathContext.DECIMAL64);
            // Capped at what is left, so rounding on earlier returns can never
            // leave the last one crediting less than nothing.
            discount = prorate(sale.getDiscountAmount(), share).min(remainingDiscount);
            taxable = prorate(sale.getTaxableAmount(), share).min(remainingTaxable);
            vat = prorate(sale.getVatAmount(), share).min(remainingVat);
        }

        salesReturn.setSubTotal(subTotal);
        salesReturn.setDiscountAmount(discount);
        salesReturn.setTaxableAmount(taxable);
        salesReturn.setVatAmount(vat);
        salesReturn.setNetTotal(taxable.add(vat));
    }

    private static BigDecimal prorate(BigDecimal amount, BigDecimal share) {
        return zeroIfNull(amount).multiply(share).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal remaining(BigDecimal onSale, List<SalesReturn> previous,
                                        Function<SalesReturn, BigDecimal> amount) {
        BigDecimal credited = previous.stream()
                .map(amount)
                .map(SalesReturnCalculator::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return zeroIfNull(onSale).subtract(credited).max(BigDecimal.ZERO)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
