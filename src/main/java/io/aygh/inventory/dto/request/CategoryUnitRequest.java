package io.aygh.inventory.dto.request;

import io.aygh.inventory.entity.UnitUsage;
import jakarta.validation.constraints.NotNull;

/** Authorises one unit for one side of the trade within a category. */
public record CategoryUnitRequest(

        @NotNull(message = "Unit is required")
        Long unitId,

        @NotNull(message = "Usage is required — PURCHASE or SELLING")
        UnitUsage usage
) {
}
