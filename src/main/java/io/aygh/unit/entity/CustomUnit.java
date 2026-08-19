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
 * Mart-defined measurement units (e.g. Sack, Crate, Bundle, Dozen-of-24).
 * Unlike {@link SystemUnit} these are owned by the mart and may be edited or
 * removed. They still convert against the reference unit of their measurement type.
 */
@Entity
@Table(name = "custom_units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomUnit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_type", nullable = false, length = 50)
    private MeasurementType measurementType;

    /**
     * Conversion factor to the reference unit of the same measurement type.
     */
    @Column(name = "conversion_factor", nullable = false, precision = 19, scale = 6)
    private BigDecimal conversionFactor;

    @Version
    private Long version;
}
