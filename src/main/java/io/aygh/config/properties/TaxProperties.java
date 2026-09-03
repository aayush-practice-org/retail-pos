package io.aygh.config.properties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * The VAT rate a sale is billed at.
 * <p>
 * One rate for the whole mart, rather than the per-product effective-dated rates
 * purchasing uses. The two are asymmetric on purpose: what a mart is
 * <em>charged</em> varies by good and by supplier and has to be recorded as it
 * was billed, whereas what it <em>charges</em> is the statutory rate of the day,
 * the same across the shelf. A rate change is a config change and a redeploy,
 * which is the right weight for something that happens with a budget.
 */
@Validated
@ConfigurationProperties(prefix = "app.tax")
public record TaxProperties(

        @DecimalMin(value = "0.0", message = "app.tax.vat-rate cannot be negative")
        @DecimalMax(value = "100.0", message = "app.tax.vat-rate cannot exceed 100")
        BigDecimal vatRate
) {

    /** Nepal's standard rate, used when nothing is configured. */
    private static final BigDecimal DEFAULT_VAT_RATE = BigDecimal.valueOf(13);

    public TaxProperties {
        vatRate = vatRate == null ? DEFAULT_VAT_RATE : vatRate;
    }
}
