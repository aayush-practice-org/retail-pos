package io.aygh.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Adds the two decisions that are made once and never revisited: which category
 * the product sits in, and which unit its stock is counted in.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductCreateRequest extends ProductBaseRequest {

    @NotNull(message = "Category is required")
    private Long categoryId;

    /**
     * The unit every stock figure is stored in. Fixed at creation: changing it
     * later would silently reinterpret every quantity already recorded.
     */
    @NotNull(message = "Base unit is required")
    private Long baseUnitId;
}
