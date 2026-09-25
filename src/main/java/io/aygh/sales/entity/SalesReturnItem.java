package io.aygh.sales.entity;

import io.aygh.inventory.entity.Product;
import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

/**
 * One line handed back, pointing at the bill line it came off.
 */
@Entity
@Table(name = "sales_return_items")
@SQLDelete(sql = "UPDATE sales_return_items SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesReturnItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_return_id", nullable = false)
    private SalesReturn salesReturn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_item_id", nullable = false)
    private SaleItem saleItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** As it read on the bill. */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "unit_symbol", nullable = false, length = 20)
    private String unitSymbol;

    /** How many of the selling unit came back. */
    @Column(name = "quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    /** What went back on the shelf, at the bill line's pack quantity. */
    @Column(name = "quantity_in_base_units", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantityInBaseUnits;

    /** The rate the bill line was charged at. */
    @Column(name = "rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal rate;

    /** This line's share of the bill line's total, line discount included. */
    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;
}
