package io.aygh.unit.entity;

import io.aygh.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Globally seeded measurement units (Gram, Kilogram, Milliliter, Liter, Piece, ...).
 * These are defined at the system level and seeded by migration — the mart may
 * NOT create or delete them; it can only add {@link CustomUnit} records.
 * <p>
 * Products are assigned one of these as their base unit; every quantity stored
 * against a product is expressed in that base unit.
 */
@Entity
@Table(name = "system_units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemUnit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_type", nullable = false, length = 50)
    private MeasurementType measurementType;

    /**
     * Conversion factor relative to the reference unit of the same measurement type.
     * e.g. Kilogram -> 1000 (because 1 kg = 1000 grams)
     * Gram -> 1 (the reference unit itself)
     */
    @Column(name = "conversion_factor", nullable = false, precision = 19, scale = 6)
    private BigDecimal conversionFactor;

    /**
     * True for Gram, Milliliter, Piece and Centimeter — the reference units.
     * Conversion math: qty_in_reference = qty * conversionFactor
     */
    @Column(name = "is_base_unit", nullable = false)
    private boolean isBaseUnit;

    @Version
    private Long version;
}
