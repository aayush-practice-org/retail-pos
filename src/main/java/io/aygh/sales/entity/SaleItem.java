package io.aygh.sales.entity;

import io.aygh.inventory.entity.Product;
import io.aygh.inventory.entity.ProductSellingUnit;
import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

/**
 * One line of a sale.
 * <p>
 * The product's name and the unit's symbol are copied onto the row alongside the
 * references. A bill reprinted after the product was renamed — or removed —
 * still has to read the way it was handed to the customer.
 */
@Entity
@Table(name = "sale_items")
@SQLDelete(sql = "UPDATE sale_items SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaleItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_selling_unit_id", nullable = false)
    private ProductSellingUnit sellingUnit;

    /** As it read on the bill. */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "unit_symbol", nullable = false, length = 20)
    private String unitSymbol;

    /** How many of the selling unit. */
    @Column(name = "quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    /** Base units per selling unit, as it stood when this line was rung up. */
    @Column(name = "pack_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal packQuantity;

    /** {@code quantity × packQuantity} — what came off the shelf. */
    @Column(name = "quantity_in_base_units", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantityInBaseUnits;

    /** Price of one selling unit, as charged. */
    @Column(name = "rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal rate;

    /** Printed price, when there was one, so the receipt can show what was saved. */
    @Column(name = "mrp", precision = 12, scale = 2)
    private BigDecimal mrp;

    /** Taken off this line before it was totalled. */
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** {@code quantity × rate − discount}. */
    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;
}
