package io.aygh.inventory.entity;

import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

/**
 * One unit a category permits, for one side of the trade: Grains may be bought
 * in Sacks and sold in Kilograms.
 * <p>
 * A join entity rather than a plain {@code @ManyToMany} because the pairing
 * carries {@link UnitUsage}, and because anything later attached to the
 * permission — a display order, who granted it — has somewhere to go.
 * Products choose their own units from the set their category authorises; this
 * table is the policy, never the product's configuration.
 */
@Entity
@Table(name = "category_units")
@SQLDelete(sql = "UPDATE category_units SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryUnit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 20)
    private UnitUsage usage;
}
