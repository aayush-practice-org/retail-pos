package io.aygh.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * How much a pack holds, in the words a shopkeeper actually uses: a sack
 * {@code contains} 50 kg.
 * <p>
 * The alternative is making them state it in the product's base unit, which for
 * anything sold by weight means typing 50000 — a number nobody says out loud,
 * derived by hand, where a slipped zero quietly misstates every stock figure
 * that ever passes through the unit. The conversion is arithmetic, so the
 * server does it.
 */
public record PackSizeRequest(

        @NotNull(message = "Pack size quantity is required")
        @DecimalMin(value = "0.000001", message = "Pack size must be greater than zero")
        BigDecimal qty,

        /** The unit the quantity is in, by symbol or name — "kg", "Kilogram". */
        String unit,

        /** The same thing by id, for a caller that already resolved it. */
        Long unitId
) {
}
