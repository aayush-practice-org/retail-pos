package io.aygh.unit.resolver;

import io.aygh.unit.entity.MeasurementType;
import io.aygh.unit.entity.UnitSource;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A unit flattened out of either {@code SystemUnit} or {@code CustomUnit},
 * so callers that only need the conversion maths do not care where it came from.
 */
public record ResolvedUnit(
        UUID id,
        String name,
        String symbol,
        MeasurementType measurementType,
        BigDecimal conversionFactor,
        UnitSource source
) {
}
