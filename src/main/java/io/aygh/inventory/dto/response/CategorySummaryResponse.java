package io.aygh.inventory.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A category as it appears in a list: enough to render a row, and nothing that
 * would need a second query per row to fill.
 */
@Getter
@Setter
@NoArgsConstructor
public class CategorySummaryResponse extends BaseResponse {

    private String name;
    private String description;
    private String image;
}
