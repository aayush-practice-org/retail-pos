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
     * How many of this measurement type's reference unit one of these is: a
     * Kilogram is 1000 where Gram is the reference.
     * <p>
     * NULL for a unit with no fixed size — a Sack, a Crate, a Carton — whose
     * contents are a decision the product makes, not a fact about the unit.
     * Those defer to the product's pack quantity, and asking this class for a
     * factor it does not have is an error rather than a silent 1.
     */
    @Column(name = "conversion_factor", precision = 19, scale = 6)
    private BigDecimal conversionFactor;

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
