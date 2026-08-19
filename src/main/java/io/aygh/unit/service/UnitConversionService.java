package io.aygh.unit.service;

import io.aygh.exception.BusinessException;
import io.aygh.unit.entity.SystemUnit;
import io.aygh.unit.entity.UnitSource;
import io.aygh.unit.resolver.ResolvedUnit;
import io.aygh.unit.resolver.UnitResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * All quantity maths in the mart funnels through here.
 * <p>
 * Every unit carries a conversion factor to the reference unit of its measurement
 * type (Gram for WEIGHT, Milliliter for VOLUME, Piece for COUNT, Centimeter for
 * LENGTH), so converting between any two units of the same type is:
 *
 * <pre>
 *   qty_in_target = qty * from.conversionFactor / target.conversionFactor
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class UnitConversionService {

    private static final int SCALE = 6;

    private final UnitResolver unitResolver;

    /**
     * Converts a quantity expressed in {@code unitId} into {@code targetUnit} —
     * typically the base unit a product is tracked in.
     * <p>
     * A null {@code unitId} means the caller already supplied the quantity in the
     * target unit, so it is returned untouched.
     */
    public BigDecimal convertToUnit(BigDecimal quantity, UUID unitId, UnitSource source, SystemUnit targetUnit) {
        if (quantity == null) {
            return null;
        }
        if (unitId == null || unitId.equals(targetUnit.getId())) {
            return quantity;
        }

        ResolvedUnit from = unitResolver.resolve(unitId, source);

        if (from.measurementType() != targetUnit.getMeasurementType()) {
            throw new BusinessException(
                    "Cannot convert %s (%s) to %s (%s) — the units measure different things"
                            .formatted(from.name(), from.measurementType(),
                                    targetUnit.getName(), targetUnit.getMeasurementType()));
        }

        return quantity
                .multiply(from.conversionFactor())
                .divide(targetUnit.getConversionFactor(), SCALE, RoundingMode.HALF_UP);
    }
}
