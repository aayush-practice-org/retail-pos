package io.aygh.catalog.entity;

import io.aygh.shared.entity.BaseEntity;
import io.aygh.unit.entity.UnitSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single sellable pack of a product — "500 ml bottle", "1 kg pouch", "pack of 6".
 * <p>
 * Variants are owned by exactly one product and are never shared between
 * products: each one carries its own SKU, barcode, pack size and pricing, and is
 * created and managed under that product alone.
 */
@Entity
@Table(
        name = "product_variants",
        indexes = {
                @Index(name = "idx_product_variants_product", columnList = "product_id"),
                @Index(name = "idx_product_variants_sku", columnList = "sku"),
                @Index(name = "idx_product_variants_barcode", columnList = "barcode")
        }
)
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE product_variants SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 64)
    @Column(length = 64)
    private String sku;

    @Size(max = 64)
    @Column(length = 64)
    private String barcode;

    /**
     * How much of the product one unit of this variant contains, always expressed
     * in the product's base unit (500 for a 500 ml bottle of a product tracked in ml).
     */
    @NotNull
    @Positive
    @Column(name = "pack_size", nullable = false, precision = 19, scale = 6)
    private BigDecimal packSize;

    /**
     * What the operator actually typed, kept for audit and for rendering the pack
     * size back the way it was entered ("1 kg" rather than "1000 g").
     */
    @Column(name = "original_quantity", precision = 19, scale = 6)
    private BigDecimal originalQuantity;

    @Column(name = "original_unit_id")
    private UUID originalUnitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_unit_source", length = 20)
    private UnitSource originalUnitSource;

    /**
     * Printed maximum retail price. Optional — not every article carries one.
     */
    @PositiveOrZero
    @Column(precision = 10, scale = 2)
    private BigDecimal mrp;

    @NotNull
    @PositiveOrZero
    @Column(name = "selling_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal sellingPrice;

    /**
     * The variant offered first at the counter. Exactly one per product.
     */
    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
