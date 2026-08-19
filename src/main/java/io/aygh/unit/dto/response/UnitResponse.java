package io.aygh.unit.dto.response;

import io.aygh.unit.entity.MeasurementType;
import io.aygh.unit.entity.UnitSource;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class UnitResponse {
    private UUID id;
    private String name;
    private String symbol;
    private MeasurementType measurementType;
    private BigDecimal conversionFactor;
    private boolean isBaseUnit;
    private UnitSource source;
}
