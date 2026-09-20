package io.aygh.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * One way a product trades, on either side of the counter.
 * <p>
 * Buying and selling share this shape rather than splitting into two records,
 * because at the point of entry they differ only in which optional fields get
 * filled: a barcode is meaningless on a purchase line, a VAT rate on a selling
 * one. Two near-identical records would have to be kept in step by hand, and
 * the sugar that expands {@code sellingPrice} into a line would need writing
 * twice. The fields that do not apply to a side are simply ignored there, and
 * the service says so when one is sent to the wrong side.
 * <p>
 * Everything except {@code price} is optional, which is what lets the common
 * case — bought and sold by the piece — be expressed by leaving nearly all of
 * it out.
 */
public record ProductUnitLineRequest(

        /** By symbol or name: "kg", "Kilogram", "Sack". Omitted means the base unit. */
        String unit,

        /** The same thing by id. Wins over {@link #unit()} when both are sent. */
        Long unitId,

        /**
         * How much one of these holds. Omitted, the server works it out: from
         * the unit's own conversion factor when it has a fixed size, and from
         * {@link #packQuantity()} when it does not.
         */
        @Valid
        PackSizeRequest contains,

        /**
         * The pack size stated directly in the product's base unit. An escape
         * hatch for a caller that has already done the arithmetic; prefer
         * {@link #contains()}, which is harder to get wrong.
         */
        @DecimalMin(value = "0.000001", message = "Pack quantity must be greater than zero")
        BigDecimal packQuantity,

        /**
         * Per one of this unit. Required on a selling line — a product with no
         * price cannot be rung up — and optional on a purchase line, where the
         * vendor's bill is the real source and this is only a default.
         */
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        BigDecimal price,

        /** Selling side only: the printed price, kept beside the real one. */
        @DecimalMin(value = "0.00", message = "MRP cannot be negative")
        BigDecimal mrp,

        /** Selling side only. */
        @Size(max = 64)
        String sku,

        /** Selling side only. */
        @Size(max = 64)
        String barcode,

        /**
         * Purchase side only. Omitted, the mart's configured rate applies, so
         * the ordinary case needs no VAT entry at all. Send 0 for exempt goods.
         */
        @DecimalMin(value = "0.00", message = "VAT rate cannot be negative")
        @DecimalMax(value = "100.00", message = "VAT rate cannot exceed 100%")
        BigDecimal vatRate,

        /**
         * The line the till and the order screen reach for. Exactly one per
         * side ends up marked: if no line claims it, the first one gets it.
         */
        Boolean isDefault
) {

    /** Whether this line names a unit at all, as opposed to meaning the base unit. */
    public boolean namesUnit() {
        return unitId != null || (unit != null && !unit.isBlank());
    }

    public boolean wantsDefault() {
        return Boolean.TRUE.equals(isDefault);
    }
}
