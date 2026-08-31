package io.aygh.inventory.entity;

import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Entity
@Table(name = "products")
@SQLDelete(sql = "UPDATE products SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "product_code", length = 64)
    private String productCode;

    @Column(name = "base_code", length = 64)
    private String baseCode;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "brand", length = 255)
    private String brand;

    @Column(name = "image", length = 255)
    private String image;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── Relations ─────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * The unit every stock figure for this product is stored in. Immutable once
     * set: changing it would silently reinterpret every quantity already
     * recorded against it.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "base_unit_id", nullable = false, updatable = false)
    private Unit baseUnit;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductPurchaseUnit> purchaseUnits = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductSellingUnit> sellingUnits = new ArrayList<>();


    public void addPurchaseUnit(ProductPurchaseUnit purchaseUnit) {
        purchaseUnits.add(purchaseUnit);
        purchaseUnit.setProduct(this);
    }

    public void removePurchaseUnit(ProductPurchaseUnit purchaseUnit) {
        purchaseUnits.remove(purchaseUnit);
        purchaseUnit.setProduct(null);
    }

    public void addSellingUnit(ProductSellingUnit sellingUnit) {
        sellingUnits.add(sellingUnit);
        sellingUnit.setProduct(this);
    }

    public void removeSellingUnit(ProductSellingUnit sellingUnit) {
        sellingUnits.remove(sellingUnit);
        sellingUnit.setProduct(null);
    }

    public Optional<ProductSellingUnit> defaultSellingUnit() {
        return sellingUnits.stream()
                .filter(ProductSellingUnit::isActive)
                .filter(ProductSellingUnit::isDefault)
                .findFirst();
    }

    public Optional<ProductPurchaseUnit> defaultPurchaseUnit() {
        return purchaseUnits.stream()
                .filter(ProductPurchaseUnit::isActive)
                .filter(ProductPurchaseUnit::isDefault)
                .findFirst();
    }
}
