package io.aygh.purchase.dto.request;

import io.aygh.shared.entity.PaymentMethod;
import io.aygh.shared.entity.TaxScheme;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Goods received from a vendor on one bill, over any number of products.
 * <p>
 * No totals are accepted from the caller. Everything but the discount is worked
 * out from the lines server-side, because a client that computes its own total
 * is a client that can disagree with the ledger.
 */
public record PurchaseRequest(

        @NotNull(message = "Vendor is required")
        Long vendorId,

        @NotBlank(message = "Bill number is required")
        @Size(max = 64)
        String billNumber,

        /** Defaults to today when the bill does not say. */
        LocalDate purchaseDate,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @NotNull(message = "Tax scheme is required")
        TaxScheme taxScheme,

        @DecimalMin(value = "0.0", message = "Discount cannot be negative")
        BigDecimal discountAmount,

        @Size(max = 255)
        String remark,

        @NotEmpty(message = "A purchase needs at least one item")
        @Valid
        List<PurchaseItemRequest> items
) {
}
