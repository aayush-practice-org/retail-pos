package io.aygh.shared.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class NepaliDateUtils {

    // Nepal timezone
    private static final ZoneId NEPAL_ZONE = ZoneId.of("Asia/Kathmandu");

    // Base structural reference index: 1944-04-13 AD matches exactly 2001-01-01 BS
    private static final LocalDate BASE_AD = LocalDate.of(1944, 4, 13);
    private static final int BASE_BS_YEAR = 2001;

    // Static calendar configuration map for calculating variable Nepali days per month
    private static final int[][] DAYS_IN_BS_MONTHS = {
            {31, 31, 32, 32, 31, 30, 30, 30, 29, 30, 29, 30}, // 2001
            {31, 32, 31, 32, 31, 30, 30, 30, 30, 29, 30, 30}, // 2002
            {31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 29, 30}, // 2003
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 29}, // 2004
            {31, 31, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2005
            {31, 32, 31, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2006
            {31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30, 30}, // 2007
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2008
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2009
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2010
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2011
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2012
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2013
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2014
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2015
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2016
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2017
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2018
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2019
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2020
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2021
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2022
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2023
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2024
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2025
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2026
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2027
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2028
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2029
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2030
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2031
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2032
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2033
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2034
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2035
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2036
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2037
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2038
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2039
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2040
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2041
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2042
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2043
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2044
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2045
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2046
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2047
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2048
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2049
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2050
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2051
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2052
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2053
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2054
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2055
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2056
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2057
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2058
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2059
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2060
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2061
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2062
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2063
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2064
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2065
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2066
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2067
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2068
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2069
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2070
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2071
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2072
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2073
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2074
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2075
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}, // 2076
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2077
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2078
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2079
            {31, 32, 32, 32, 31, 30, 32, 30, 30, 30, 30, 30}, // 2080
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2081
            {31, 31, 32, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2082
            {31, 32, 31, 32, 31, 31, 30, 30, 30, 30, 30, 30}, // 2083
            {31, 32, 32, 31, 31, 31, 30, 30, 30, 30, 30, 30}, // 2084
            {31, 32, 32, 32, 31, 30, 30, 30, 30, 30, 30, 30}  // 2085
    };

    /**
     * Inner data carrier class representing parsed Nepali calendar units.
     */
    public static class NepalDate {
        public final int year;
        public final int month;
        public final int day;

        public NepalDate(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }
    }

    /**
     * Converts an Instant into Nepal's local date.
     * <p>
     * The Instant represents an absolute point in time.
     * It is converted to Asia/Kathmandu first so the correct Nepal date is used.
     */
    private static LocalDate toNepalDate(Instant instant) {
        return instant
                .atZone(NEPAL_ZONE)
                .toLocalDate();
    }

    /**
     * Internal extraction method to process absolute day metrics into a structured object.
     */
    public static NepalDate getNepaliDateObject(LocalDate adDate) {
        if (adDate.isBefore(BASE_AD)) {
            throw new IllegalArgumentException(
                    "System timestamp predates supported Nepali business calendar maps."
            );
        }

        long totalDaysDelta = ChronoUnit.DAYS.between(BASE_AD, adDate);

        int bsYear = BASE_BS_YEAR;
        int bsMonth = 0;

        while (true) {
            int yearIndex = bsYear - BASE_BS_YEAR;

            if (yearIndex >= DAYS_IN_BS_MONTHS.length) {
                throw new IllegalStateException(
                        "Calendar dataset requires generation matrix update."
                );
            }

            int daysInThisYear = 0;

            for (int m = 0; m < 12; m++) {
                daysInThisYear += DAYS_IN_BS_MONTHS[yearIndex][m];
            }

            if (totalDaysDelta >= daysInThisYear) {
                totalDaysDelta -= daysInThisYear;
                bsYear++;
            } else {
                break;
            }
        }

        int yearIndex = bsYear - BASE_BS_YEAR;

        for (int m = 0; m < 12; m++) {
            int daysInThisMonth = DAYS_IN_BS_MONTHS[yearIndex][m];

            if (totalDaysDelta >= daysInThisMonth) {
                totalDaysDelta -= daysInThisMonth;
            } else {
                bsMonth = m + 1;
                break;
            }
        }

        int bsDay = (int) totalDaysDelta + 1;

        return new NepalDate(bsYear, bsMonth, bsDay);
    }

    /**
     * Converts an Instant into the IRD standard "YYYY.MM.DD" BS date.
     */
    public static String getIrdCompliantBsDate(Instant instant) {
        LocalDate nepalDate = toNepalDate(instant);

        NepalDate bs = getNepaliDateObject(nepalDate);

        return String.format(
                "%04d.%02d.%02d",
                bs.year,
                bs.month,
                bs.day
        );
    }

    /**
     * Formats the active budget boundary matching the required structural pattern.
     * <p>
     * Rules:
     * Roll over occurs on Shrawan 1st (Month 04).
     */
    public static String getIrdFiscalYear(Instant instant) {
        LocalDate nepalDate = toNepalDate(instant);
        NepalDate bs = getNepaliDateObject(nepalDate);

        int startYear;
        int endYear;

        // In a 1-based index calendar:
        // Baisakh=1, Jetha=2, Asar=3, Shrawan=4.
        // If month is Shrawan (4) or higher, it starts a new fiscal year.
        if (bs.month >= 4) {
            startYear = bs.year;
            endYear = bs.year + 1;
        } else {
            startYear = bs.year - 1;
            endYear = bs.year;
        }

        // Extract trailing two digits of fiscal cycle end-cap.
        int shortEndYear = endYear % 100;

        return String.format(
                "%04d.%03d",
                startYear,
                shortEndYear
        );
    }

    public static String getBillingFiscalYear(String fiscalYear) {


        // 3. Split the "2083.084" string by the literal dot
        String[] parts = fiscalYear.split("\\.");
        String startYearFull = parts[0];      // "2083"
        String endYearThreeDigit = parts[1];  // "084"

        // 4. Extract short 2-digit variations: "83" and "84"
        String shortStart = startYearFull.substring(2);
        String shortEnd = endYearThreeDigit.substring(1);

        // Returns standard billing prefix format: "83/84-"
        return String.format("%s/%s-", shortStart, shortEnd);
    }

}