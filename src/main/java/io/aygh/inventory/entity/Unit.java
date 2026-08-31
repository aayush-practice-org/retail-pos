package io.aygh.inventory.entity;

import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

/*
 * These are the units that the mart uses to measure products.
 * Examples:
 *  Gram
 *  Kilogram
 *  Piece
 *  Dozen
 *  Sack
 *  Crate
 * */
@Entity
@Table(name = "units")
@SQLDelete(sql = "UPDATE units SET deleted_at = NOW() WHERE id = ?")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Unit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    /**
     * The type of measurement this unit represents.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_type", nullable = false, length = 20)
    private MeasurementType measurementType;

    /**
     * How many of this measurement type's reference unit fit in one of these:
     * Kilogram carries 1000 against a Gram reference. Only meaningful for units
     * with a fixed size — a Sack holds whatever the product says it holds, so
     * it carries 1 here and defers to
     * {@link ProductPurchaseUnit#getPackQuantity()}.
     */
    @Column(name = "conversion_factor", nullable = false, precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal conversionFactor = BigDecimal.ONE;

    /**
     * True for the one unit each measurement type is expressed in.
     */
    @Column(name = "reference_unit", nullable = false)
    @Builder.Default
    private boolean referenceUnit = false;

    /**
     * Seeded by migration and not editable by the mart, versus mart-defined.
     */
    @Column(name = "system_defined", nullable = false, updatable = false)
    @Builder.Default
    private boolean systemDefined = false;
}
