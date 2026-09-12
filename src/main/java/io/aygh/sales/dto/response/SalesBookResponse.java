package io.aygh.sales.dto.response;

import java.util.List;

/**
 * The IRD sales book for one period: the firm's own details, a line per bill, and the
 * column totals.
 * <p>
 * {@code month} and {@code year} are echoed back from the request — the period is a
 * Nepali (BS) one, which the server cannot derive from the AD date range it filters on.
 */
public record SalesBookResponse(
        String firmName,
        String pan,
        String month,
        String year,
        List<SalesBookRowResponse> rows,
        SalesBookTotalResponse total,
        String duration
) {
    public SalesBookResponse(
            String firmName,
            String pan,
            String month,
            String year,
            List<SalesBookRowResponse> rows,
            SalesBookTotalResponse total
    ) {
        this(firmName, pan, month, year, rows, total, formatDuration(month, year));
    }

    private static String formatDuration(String month, String year) {
        boolean hasMonth = month != null && !month.isBlank();
        boolean hasYear = year != null && !year.isBlank();
        if (hasMonth && hasYear) {
            return "Month " + month + "   Year " + year;
        }
        if (hasMonth) {
            return "Month " + month;
        }
        if (hasYear) {
            return "Year " + year;
        }
        return null;
    }
}
