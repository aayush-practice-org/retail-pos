package io.aygh.stock.entity;

import io.aygh.inventory.entity.Product;
import io.aygh.inventory.entity.Unit;
import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

/**
 * One movement of stock — the ledger every quantity in the mart is derived from.
 * <p>
 * Append-only, like the vendor ledger: stock that went out is never edited away,
 * it is brought back by a movement in the other direction. That is what lets
 * "why does this product say 12" be answered.
 * <p>
 * {@code quantity} is always positive and in the product's base unit, so a sack
 * bought and a kilo sold are directly comparable. What the operator actually
 * typed is kept alongside in {@code enteredQuantity} / {@code enteredUnit},
 * because "you removed 2" reads very differently from "you removed 100000" when
 * the operator chose sacks.
 */
@Entity
@Table(name = "stock_movements")
@SQLDelete(sql = "UPDATE stock_movements SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockMovement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private StockMovementType movementType;

    /** Positive, in the product's base unit. The type carries the direction. */
    @Column(name = "quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    /**
     * The product's stock immediately after this movement, in base units. Stored
     * rather than recomputed so a ledger page renders without a running sum, and
     * so a drift between ledger and cache is visible rather than silent.
     */
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 6)
    private BigDecimal balanceAfter;

    // ── What the operator actually entered ────────────────────────────────

    @Column(name = "entered_quantity", precision = 19, scale = 6)
    private BigDecimal enteredQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entered_unit_id")
    private Unit enteredUnit;

    // ── Where it came from ────────────────────────────────────────────────

    /** The purchase or sale that caused this. Null for a standalone movement. */
    @Column(name = "reference_id")
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 20)
    private StockReferenceType referenceType;

    @Column(name = "remark", length = 255)
    private String remark;
}
