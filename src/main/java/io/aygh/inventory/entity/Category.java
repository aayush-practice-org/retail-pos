package io.aygh.inventory.entity;

import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "categories")
@SQLDelete(sql = "UPDATE categories SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "image", length = 255)
    private String image;

    /**
     * Where a product goes when nobody chose an aisle. Exactly one category
     * carries it, enforced by a partial unique index — quick-add at the till
     * has to have somewhere to put things, and "somewhere" cannot be ambiguous.
     */
    @Column(name = "default_category", nullable = false)
    @Builder.Default
    private boolean defaultCategory = false;

    /**
     * The units products in this category may be traded in. Cascaded: a
     * permission has no meaning once the category is gone.
     */
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CategoryUnit> categoryUnits = new ArrayList<>();

    /**
     * Inverse side, deliberately not cascaded: removing a category must never
     * take its products down with it.
     */
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Product> products = new ArrayList<>();


    public void allowUnit(Unit unit, UnitUsage usage) {
        CategoryUnit permission = CategoryUnit.builder()
                .category(this)
                .unit(unit)
                .usage(usage)
                .build();
        categoryUnits.add(permission);
    }

    /**
     * Whether this category authorises {@code unit} for {@code usage}.
     */
    public boolean permits(Unit unit, UnitUsage usage) {
        return categoryUnits.stream()
                .anyMatch(cu -> cu.getUsage() == usage
                        && cu.getUnit() != null
                        && cu.getUnit().getId() != null
                        && cu.getUnit().getId().equals(unit.getId()));
    }
}
