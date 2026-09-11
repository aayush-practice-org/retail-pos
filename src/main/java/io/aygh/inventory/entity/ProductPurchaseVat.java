package io.aygh.inventory.entity;

import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The VAT rate applied to buying a product in a given purchase unit, over a
 * window of time.
 * <p>
 * Its own table rather than a column on {@link ProductPurchaseUnit} because a
 * rate is a fact about a period, not about the row: when the government moves
 * VAT from 13% to 15%, a column overwrites what last quarter's purchases were
 * taxed at and takes the audit trail with it. A new row leaves history intact.
 */
@Entity
@Table(name = "product_purchase_vats")
@SQLDelete(sql = "UPDATE product_purchase_vats SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductPurchaseVat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_purchase_unit_id", nullable = false)
    private ProductPurchaseUnit productPurchaseUnit;

    /** Percentage, not a fraction: 13 means 13%. */
    @Column(name = "rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal rate;

}
