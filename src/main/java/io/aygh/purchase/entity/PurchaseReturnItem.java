package io.aygh.purchase.entity;

import io.aygh.inventory.entity.Product;
import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

/**
 * One line sent back, pointing at the bill line it was bought on.
 */
@Entity
@Table(name = "purchase_return_items")
@SQLDelete(sql = "UPDATE purchase_return_items SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseReturnItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_return_id", nullable = false)
    private PurchaseReturn purchaseReturn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_item_id", nullable = false)
    private PurchaseItem purchaseItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** How many of the purchase unit went back. */
    @Column(name = "quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    /** What came off the shelf, at the bill line's pack quantity. */
    @Column(name = "quantity_in_base_units", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantityInBaseUnits;

    /** The rate the bill line was bought at. */
    @Column(name = "rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal rate;

    /** This line's share of the bill line's total. */
    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;
}
