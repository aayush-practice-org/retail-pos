package io.aygh.catalog.entity;

import io.aygh.shared.entity.BaseEntity;
import io.aygh.unit.entity.SystemUnit;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/**
 * A sellable article in the mart catalogue.
 * <p>
 * A product is the shelf-level concept ("Coca-Cola", "Basmati Rice"); what is
 * actually scanned and sold is one of its {@link ProductVariant}s. Every
 * quantity attached to a product is expressed in its {@link SystemUnit} base
 * unit, which is chosen from the system-defined set.
 */
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_category", columnList = "category_id"),
                @Index(name = "idx_products_base_unit", columnList = "base_unit_id")
        }
)
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE products SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @Size(max = 255)
    private String brand;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * The system-defined unit every quantity of this product is tracked in.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "base_unit_id", nullable = false)
    private SystemUnit baseUnit;

    @Column(name = "image_url")
    private String imageUrl;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();
}
