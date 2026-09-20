package io.aygh.inventory.helper;

import io.aygh.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The file arrives from somebody else's spreadsheet, so the parser's job is
 * mostly to be unsurprised by it.
 */
class CsvTableTest {

    @Test
    void readsAPlainFile() {
        CsvTable table = CsvTable.parse("name,sell_price\nSugar,145\nRice,120\n");

        assertEquals(2, table.size());
        assertEquals("Sugar", table.row(0).get("name"));
        assertEquals("145", table.row(0).get("sell_price"));
        assertEquals("Rice", table.row(1).get("name"));
    }

    @Test
    void headersMatchHoweverTheyWereSpelt() {
        CsvTable table = CsvTable.parse("Product Name,Sell Price\nSugar,145\n");

        // All three spellings normalise to the same column.
        assertEquals("Sugar", table.row(0).get("productname"));
        assertEquals("145", table.row(0).get("sell_price"));
        assertEquals("145", table.row(0).get("sellPrice"));
    }

    @Test
    void anAliasFallsThroughToTheNextSpelling() {
        CsvTable table = CsvTable.parse("name,rate\nSugar,145\n");

        assertEquals("145", table.row(0).get("sellprice", "sellingprice", "price", "rate"));
    }

    @Test
    void quotedFieldsMayHoldCommasAndNewlines() {
        CsvTable table = CsvTable.parse("name,description\n\"Rice, Basmati\",\"Long grain\nAged\"\n");

        assertEquals(1, table.size());
        assertEquals("Rice, Basmati", table.row(0).get("name"));
        assertEquals("Long grain\nAged", table.row(0).get("description"));
    }

    @Test
    void aDoubledQuoteIsALiteralOne() {
        CsvTable table = CsvTable.parse("name\n\"Milk \"\"Full Cream\"\"\"\n");

        assertEquals("Milk \"Full Cream\"", table.row(0).get("name"));
    }

    @Test
    void windowsLineEndingsReadTheSame() {
        CsvTable table = CsvTable.parse("name,sell_price\r\nSugar,145\r\nRice,120\r\n");

        assertEquals(2, table.size());
        assertEquals("Sugar", table.row(0).get("name"));
        assertEquals("120", table.row(1).get("sell_price"));
    }

    @Test
    void theByteOrderMarkExcelWritesIsIgnored() {
        CsvTable table = CsvTable.parse("﻿name,sell_price\nSugar,145\n");

        assertTrue(table.has("name"), "a BOM must not become part of the first header");
        assertEquals("Sugar", table.row(0).get("name"));
    }

    @Test
    void blankLinesAreNotRows() {
        CsvTable table = CsvTable.parse("name,sell_price\nSugar,145\n\n,\nRice,120\n");

        assertEquals(2, table.size());
    }

    @Test
    void blankAndAbsentCellsAnswerTheSame() {
        CsvTable table = CsvTable.parse("name,brand\nSugar,   \n");

        assertNull(table.row(0).get("brand"), "a blank cell reads as omitted");
        assertNull(table.row(0).get("nosuchcolumn"));
    }

    @Test
    void aShortRowDoesNotFallOffTheEnd() {
        CsvTable table = CsvTable.parse("name,brand,sell_price\nSugar\n");

        assertEquals("Sugar", table.row(0).get("name"));
        assertNull(table.row(0).get("sell_price"));
    }

    @Test
    void lineNumbersAreWhatASpreadsheetShows() {
        CsvTable table = CsvTable.parse("name\nSugar\nRice\n");

        assertEquals(2, table.row(0).lineNumber(), "the header is line 1");
        assertEquals(3, table.row(1).lineNumber());
    }

    @Test
    void anEmptyFileIsRefused() {
        assertThrows(BusinessException.class, () -> CsvTable.parse(""));
    }

    @Test
    void aHeaderWithNoRowsIsAnEmptyTable() {
        assertEquals(0, CsvTable.parse("name,sell_price\n").size());
    }
}
