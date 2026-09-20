package io.aygh.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Everything it takes to put a product on the shelf, in one request.
 * <p>
 * This used to be the first of four calls — create the product, then add a
 * purchase unit, then a selling unit, having first authorised both units on the
 * category. For the nine products in ten that are simply bought and sold by the
 * piece, three of those four calls existed to say "one", and a mart opening
 * with 800 lines paid that toll 800 times.
 * <p>
 * So the shape is one schema filled to three depths:
 * <pre>
 *   { name, categoryId, barcode, purchasePrice, sellingPrice }
 *
 *   { name, categoryId, sellIn: [{ unit: "kg", price: 145 }],
 *                       buyIn:  [{ unit: "kg", price: 120 }] }
 *
 *   { name, categoryId,
 *     sellIn: [{ unit: "kg", price: 145, isDefault: true },
 *              { unit: "g",  price: 0.15 }],
 *     buyIn:  [{ unit: "Sack", contains: { qty: 50, unit: "kg" }, price: 5800 }] }
 * </pre>
 * The first is the third with the defaults left out, not a separate endpoint:
 * one path to test, one place for the rules to live, and nothing for a mart
 * that never sells anything loose to learn.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductCreateRequest extends ProductBaseRequest {

    @NotNull(message = "Category is required")
    private Long categoryId;

    /**
     * The unit stock is counted in. Almost always left out: it is derived from
     * how the product is sold, because it is not a decision a shopkeeper is
     * equipped to make — it is immutable once set, and getting it wrong makes
     * every quantity ever recorded against the product incomparable with the
     * rest of the catalogue.
     * <p>
     * Still accepted so existing callers keep working, but now checked: it has
     * to be a reference unit. Sending "Sack" here used to be silently allowed.
     */
    private Long baseUnitId;

    // ── Shorthand, for a product bought and sold one at a time ────────────
    //
    // Each expands to a single line in the base unit. Sending both a shorthand
    // and its list is rejected rather than merged: the two would disagree about
    // which is the default, and guessing is worse than asking.

    @DecimalMin(value = "0.00", message = "Purchase price cannot be negative")
    private BigDecimal purchasePrice;

    @DecimalMin(value = "0.00", message = "Selling price cannot be negative")
    private BigDecimal sellingPrice;

    @DecimalMin(value = "0.00", message = "MRP cannot be negative")
    private BigDecimal mrp;

    @Size(max = 64)
    private String sku;

    @Size(max = 64)
    private String barcode;

    // ── The full form ─────────────────────────────────────────────────────

    /** How the product is sold. Empty is allowed: price it later. */
    @Valid
    private List<ProductUnitLineRequest> sellIn;

    /** How the product is bought. */
    @Valid
    private List<ProductUnitLineRequest> buyIn;
}
