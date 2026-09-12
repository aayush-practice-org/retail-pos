package io.aygh.sales.dto.request;

import io.aygh.sales.entity.SaleChannel;
import io.aygh.shared.entity.PaymentMethod;
import io.aygh.shared.entity.TaxScheme;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * A basket to ring up.
 * <p>
 * No totals are accepted from the caller: everything but the discounts is worked
 * out server-side from the lines, so a till that computes its own total can
 * never put a different number on the bill than the one in the books.
 */
public record SaleRequest(

        @NotNull(message = "Tax scheme is required")
        TaxScheme taxScheme,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        /** Defaults to the till. */
        SaleChannel channel,

        /** What was handed over. Left out, the bill is raised unpaid. */
        @DecimalMin(value = "0.0", message = "Tendered amount cannot be negative")
        BigDecimal tenderedAmount,

        /** Taken off the whole bill, after the per-line discounts. */
        @DecimalMin(value = "0.0", message = "Discount cannot be negative")
        BigDecimal discountAmount,

        /** Optional for cash sales, mandatory for CREDIT sales. */
        Long customerId,

        @Size(max = 150) String customerName,
        @Size(max = 30) String customerPhone,

        /** Required on a VAT bill to a registered buyer; the mart decides, not this. */
        @Size(max = 30) String customerPan,

        /**
         * The BS date the operator entered at the till, e.g. "2081.09.05".
         * Stored verbatim on the sale for invoice and IRD sales book printing.
         */
        @Size(max = 20) String nepaliDate,

        @Size(max = 255) String remark,

        @NotEmpty(message = "A sale needs at least one item")
        @Valid
        List<SaleItemRequest> items
) {
}
