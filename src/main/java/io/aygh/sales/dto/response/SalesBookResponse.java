package io.aygh.sales.dto.response;

import java.util.List;

/**
 * The IRD sales book for a date range period: the firm's details, duration, a line per bill, and column totals.
 */
public record SalesBookResponse(
        String firmName,
        String pan,
        String duration,
        List<SalesBookRowResponse> rows,
        SalesBookTotalResponse total
) {
}
