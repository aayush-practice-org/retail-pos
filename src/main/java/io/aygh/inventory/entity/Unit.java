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
