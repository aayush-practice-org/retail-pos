package io.aygh.inventory.dto.response;

import java.util.List;

/**
 * What a spreadsheet did, row by row.
 * <p>
 * The file is checked in full before anything is written, so a bad import comes
 * back as a list of every problem in it rather than the first one. Fixing a
 * 600-line file one error per upload is not a workflow anybody completes.
 *
 * @param rows     data rows found, header excluded
 * @param imported how many were written — zero whenever {@code errors} is non-empty
 * @param dryRun   true when the caller only asked for the file to be checked
 * @param errors   every problem found, in file order
 */
public record ProductImportResponse(
        int rows,
        int imported,
        boolean dryRun,
        List<RowError> errors
) {

    /**
     * @param line the line number a spreadsheet shows, header counted
     */
    public record RowError(int line, String product, String message) {
    }

    public static ProductImportResponse rejected(int rows, boolean dryRun, List<RowError> errors) {
        return new ProductImportResponse(rows, 0, dryRun, List.copyOf(errors));
    }

    public static ProductImportResponse checked(int rows) {
        return new ProductImportResponse(rows, 0, true, List.of());
    }

    public static ProductImportResponse imported(int rows) {
        return new ProductImportResponse(rows, rows, false, List.of());
    }

    public boolean clean() {
        return errors.isEmpty();
    }
}
