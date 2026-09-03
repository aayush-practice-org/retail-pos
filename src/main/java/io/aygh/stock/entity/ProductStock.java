package io.aygh.stock.entity;

import io.aygh.inventory.entity.Product;
import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

/**
 * What is on the shelf for one product, in that product's base unit.
 * <p>
 * Strictly a cache of the {@link StockMovement} ledger: it exists so the till
 * can answer "is there any left" with one indexed read instead of summing every
 * movement ever recorded. The ledger is the truth, and
 * {@code StockLedgerService} is the only thing allowed to move this figure, so
 * the two cannot drift.
 * <p>
 * The {@code version} column is the point of the row. Two tills selling the last
 * item at the same moment both read the same quantity; optimistic locking is
 * what makes the second write fail instead of overselling. It is the one entity
 * in the mart that carries one, because it is the one row genuinely contended
 * for.
 */
@Entity
@Table(name = "product_stocks")
@SQLDelete(sql = "UPDATE product_stocks SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductStock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    /** On hand, in {@link Product#getBaseUnit()}. */
    @Column(name = "quantity", nullable = false, precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;

    /**
     * The level at or below which the product is reported as low. Zero means the
     * mart has not set one, and the product is only ever reported out of stock.
     */
    @Column(name = "reorder_level", nullable = false, precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public boolean covers(BigDecimal wanted) {
        return quantity.compareTo(wanted) >= 0;
    }

    public boolean isOutOfStock() {
        return quantity.signum() <= 0;
    }

    /** Low but not gone — an out-of-stock product is reported as that instead. */
    public boolean isLow() {
        return !isOutOfStock()
                && reorderLevel.signum() > 0
                && quantity.compareTo(reorderLevel) <= 0;
    }
}
