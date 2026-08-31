package io.aygh.inventory.entity;

import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

/**
 * A unit one product is actually sold in, and what selling it earns. The mirror
 * of {@link ProductPurchaseUnit}: buy a 50kg Sack, sell by the Kilogram, and
 * {@link #packQuantity} on each side is what reconciles the two against one
 * stock figure.
 */
@Entity
@Table(name = "product_selling_units")
@SQLDelete(sql = "UPDATE product_selling_units SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductSellingUnit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    /**
     * How many of the product's base unit one of these holds.
     */
    @Column(name = "pack_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal packQuantity;

    @Column(name = "selling_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    /**
     * Printed price, kept beside the actual one so a discount is visible.
     */
    @Column(name = "mrp", precision = 12, scale = 2)
    private BigDecimal mrp;

    @Column(name = "sku", length = 64)
    private String sku;

    @Column(name = "barcode", length = 64)
    private String barcode;

    /**
     * The unit the till reaches for when a product is scanned.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
