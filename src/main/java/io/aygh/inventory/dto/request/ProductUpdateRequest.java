package io.aygh.inventory.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Editing an existing product. Carries no category and no base unit — see
 * {@link ProductCreateRequest} for why they are absent rather than ignored.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductUpdateRequest extends ProductBaseRequest {

    /** Delist without deleting. */
    private boolean active = true;
}
