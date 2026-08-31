package io.aygh.inventory.entity;

import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "product_purchase_units")
@SQLDelete(sql = "UPDATE product_purchase_units SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductPurchaseUnit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    /**
     * How many of the product's {@link Product#getBaseUnit()} one of these
     * holds. Every stock movement is converted through this before it is
     * written, so the ledger only ever holds base units and a purchase in
     * sacks and a sale in grams stay comparable.
     */
    @Column(name = "pack_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal packQuantity;

    @Column(name = "purchase_price", precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    /**
     * The unit ordering defaults to when nothing else is chosen.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * VAT rates that have applied to buying this product in this unit, newest
     * last. Effective-dated rather than a single column so a rate change does
     * not rewrite what past purchases were taxed at.
     */
    @OneToMany(mappedBy = "productPurchaseUnit", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductPurchaseVat> vatRates = new ArrayList<>();

    public void addVatRate(ProductPurchaseVat rate) {
        vatRates.add(rate);
        rate.setProductPurchaseUnit(this);
    }
}
