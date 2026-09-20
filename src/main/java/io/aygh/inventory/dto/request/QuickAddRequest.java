package io.aygh.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * A product invented at the till, mid-queue, because something scanned and
 * nothing came back.
 * <p>
 * The alternative is what usually happens instead: the cashier rings the item
 * up as something else of roughly the right price, or waves it through, and the
 * catalogue stays wrong for as long as nobody does a back-office session about
 * it. Two fields and a scan is cheap enough to do with a customer waiting,
 * which is the only moment anyone actually knows the product is missing.
 * <p>
 * Everything omitted is filled the way {@link ProductCreateRequest} fills it:
 * counted in pieces, sold one at a time, filed under the default category, at
 * the mart's VAT rate. Someone tidies it up later, from a list of things that
 * demonstrably sell.
 */
public record QuickAddRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 255)
        String name,

        @NotNull(message = "A selling price is required — the point is to ring it up")
        @DecimalMin(value = "0.00", message = "Selling price cannot be negative")
        BigDecimal sellingPrice,

        /** Whatever was just scanned, so the next scan finds it. */
        @Size(max = 64)
        String barcode,

        /** Known at the till surprisingly often, from the box it came out of. */
        @DecimalMin(value = "0.00", message = "Purchase price cannot be negative")
        BigDecimal purchasePrice,

        /** Omitted, it goes to the default category for someone to file later. */
        Long categoryId
) {
}
