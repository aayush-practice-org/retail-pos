package io.aygh.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductSellingUnitCreateRequest extends ProductSellingUnitBaseRequest {

    @NotNull(message = "Unit is required")
    private Long unitId;
}
