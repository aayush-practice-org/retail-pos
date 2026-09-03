package io.aygh.purchase.entity;

import io.aygh.inventory.entity.Product;
import io.aygh.inventory.entity.ProductPurchaseUnit;
import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

/**
 * One line of a purchase: so many of a product, in one of the units that product
 * is bought in, at a price.
 * <p>
 * Both the quantity as entered and the quantity in base units are stored. The
 * first is what the bill says and what a reprint has to show; the second is what
 * went onto the shelf, and keeping it means a stock figure can be reconciled
 * against the purchases behind it without re-reading a pack quantity that may
 * have been edited since.
 */
@Entity
@Table(name = "purchase_items")
@SQLDelete(sql = "UPDATE purchase_items SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * The unit bought in — a Sack, a Carton. Its {@code packQuantity} at the time
     * of purchase is copied to {@link #packQuantity} rather than read back later.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_purchase_unit_id", nullable = false)
    private ProductPurchaseUnit purchaseUnit;

    /** How many of the purchase unit, as written on the bill. */
    @Column(name = "quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    /** Base units per purchase unit, as it stood when this line was recorded. */
    @Column(name = "pack_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal packQuantity;

    /** {@code quantity × packQuantity} — what actually went onto the shelf. */
    @Column(name = "quantity_in_base_units", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantityInBaseUnits;

    /** Price of one purchase unit, before VAT. */
    @Column(name = "rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal rate;

    /** {@code quantity × rate}. */
    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;
}
