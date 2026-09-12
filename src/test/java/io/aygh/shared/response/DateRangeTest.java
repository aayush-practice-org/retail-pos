package io.aygh.shared.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DateRangeTest {

    @Test
    void allDateRangeValuesShouldProduceValidStartAndEnd() {
        for (DateRange range : DateRange.values()) {
            assertNotNull(range.getStart(), "getStart should not be null for " + range);
            assertNotNull(range.getEnd(), "getEnd should not be null for " + range);
            assertNotNull(range.getStartDate(), "getStartDate should not be null for " + range);
            assertNotNull(range.getEndDate(), "getEndDate should not be null for " + range);
            assertTrue(range.getStart().isBefore(range.getEnd()) || range.getStart().equals(range.getEnd()),
                    "Start should be before or equal to End for " + range);
        }
    }

    @Test
    void nullDateRangeHelpersShouldReturnNull() {
        assertNull(DateRange.getStart(null));
        assertNull(DateRange.getEnd(null));
        assertNull(DateRange.getStartDate(null));
        assertNull(DateRange.getEndDate(null));
    }
}
