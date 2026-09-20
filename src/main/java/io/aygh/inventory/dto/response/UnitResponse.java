package io.aygh.inventory.dto.response;

import io.aygh.inventory.entity.MeasurementType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One entry from the mart's unit dictionary. */
@Getter
@Setter
@NoArgsConstructor
public class UnitResponse extends BaseResponse {

    private String name;
    private String symbol;
    private MeasurementType measurementType;

    /**
     * Null means the unit has no fixed size, which is the UI's cue that a
     * product configuring it has to state a pack size.
     */
    private BigDecimal conversionFactor;

    private boolean referenceUnit;

    /** Seeded units are read-only; the UI uses this to hide the edit control. */
    private boolean systemDefined;
}
