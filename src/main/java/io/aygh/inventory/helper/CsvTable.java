package io.aygh.inventory.helper;

import io.aygh.exception.BusinessException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A spreadsheet someone exported, read leniently.
 * <p>
 * Hand-rolled rather than pulled in as a dependency because the job is small
 * and the interesting part is not the parsing: it is that the file comes from a
 * shopkeeper's laptop, so the header might say "Sell Price", "sell_price" or
 * "sellPrice", the file might open with a BOM because Excel wrote it, and the
 * line endings might be either. Matching on a normalised header — letters and
 * digits only, folded to lower case — makes all of those the same column, which
 * is the difference between an import that works first time and one that comes
 * back with "unknown column" and no explanation of what was expected.
 * <p>
 * Quoting follows RFC 4180: a field may be quoted, a quoted field may contain
 * commas and newlines, and a doubled quote inside one is a literal quote.
 */
public final class CsvTable {

    private final Map<String, Integer> columns = new LinkedHashMap<>();
    private final List<List<String>> rows;

    private CsvTable(List<String> headers, List<List<String>> rows) {
        for (int i = 0; i < headers.size(); i++) {
            columns.putIfAbsent(normalise(headers.get(i)), i);
        }
        this.rows = rows;
    }

    public static CsvTable parse(String text) {
        List<List<String>> all = split(stripBom(text));
        if (all.isEmpty()) {
            throw new BusinessException("The file is empty");
        }
        List<List<String>> body = new ArrayList<>(all.subList(1, all.size()));
        body.removeIf(CsvTable::isBlankRow);
        return new CsvTable(all.get(0), body);
    }

    public int size() {
        return rows.size();
    }

    /** Whether any of these spellings of a column is present. */
    public boolean has(String... aliases) {
        for (String alias : aliases) {
            if (columns.containsKey(normalise(alias))) {
                return true;
            }
        }
        return false;
    }

    public Row row(int index) {
        return new Row(rows.get(index), index + 2);
    }

    /** One line of the file, and the line number a spreadsheet would show for it. */
    public final class Row {

        private final List<String> values;
        private final int lineNumber;

        private Row(List<String> values, int lineNumber) {
            this.values = values;
            this.lineNumber = lineNumber;
        }

        /** The header row is line 1, so the first data row is line 2. */
        public int lineNumber() {
            return lineNumber;
        }

        /**
         * The first of these columns that is present and non-blank, trimmed.
         * Blank and absent are the same answer on purpose: a spreadsheet full
         * of empty optional cells should behave like one that omitted them.
         */
        public String get(String... aliases) {
            for (String alias : aliases) {
                Integer at = columns.get(normalise(alias));
                if (at != null && at < values.size()) {
                    String value = values.get(at).trim();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
            return null;
        }
    }

    // ── Parsing ───────────────────────────────────────────────────────────

    private static String normalise(String header) {
        return header == null ? "" : header.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static String stripBom(String text) {
        return text.startsWith("﻿") ? text.substring(1) : text;
    }

    private static boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(value -> value == null || value.isBlank());
    }

    private static List<List<String>> split(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c != '"') {
                    field.append(c);
                } else if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = false;
                }
                continue;
            }
            switch (c) {
                case '"' -> quoted = true;
                case ',' -> {
                    row.add(field.toString());
                    field.setLength(0);
                }
                // Swallowed: the newline that follows does the work, so CRLF
                // and LF files parse identically.
                case '\r' -> {
                }
                case '\n' -> {
                    row.add(field.toString());
                    field.setLength(0);
                    rows.add(row);
                    row = new ArrayList<>();
                }
                default -> field.append(c);
            }
        }
        if (!field.isEmpty() || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }
}
