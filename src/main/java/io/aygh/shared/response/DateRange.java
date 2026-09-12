package io.aygh.shared.response;


import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

public enum DateRange {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    ALL_TIME;

    private static final ZoneId ZONE = ZoneId.of("UTC");

    public Instant getStart() {
        ZonedDateTime now = ZonedDateTime.now(ZONE);

        return switch (this) {
            case TODAY -> now.toLocalDate()
                    .atStartOfDay(ZONE)
                    .toInstant();

            case YESTERDAY -> now.toLocalDate()
                    .minusDays(1)
                    .atStartOfDay(ZONE)
                    .toInstant();

            case THIS_WEEK -> now.with(
                            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .toLocalDate()
                    .atStartOfDay(ZONE)
                    .toInstant();

            case THIS_MONTH -> now.with(
                            TemporalAdjusters.firstDayOfMonth())
                    .toLocalDate()
                    .atStartOfDay(ZONE)
                    .toInstant();

            case THIS_YEAR -> now.with(
                            TemporalAdjusters.firstDayOfYear())
                    .toLocalDate()
                    .atStartOfDay(ZONE)
                    .toInstant();

            case ALL_TIME -> Instant.parse("1970-01-01T00:00:00Z");
        };
    }

    public Instant getEnd() {
        ZonedDateTime now = ZonedDateTime.now(ZONE);

        return switch (this) {
            case TODAY -> now.toLocalDate()
                    .plusDays(1)
                    .atStartOfDay(ZONE)
                    .minusNanos(1)
                    .toInstant();

            case YESTERDAY -> now.toLocalDate()
                    .atStartOfDay(ZONE)
                    .minusNanos(1)
                    .toInstant();

            case THIS_WEEK -> now.with(
                            TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    .toLocalDate()
                    .plusDays(1)
                    .atStartOfDay(ZONE)
                    .minusNanos(1)
                    .toInstant();

            case THIS_MONTH -> now.with(
                            TemporalAdjusters.lastDayOfMonth())
                    .toLocalDate()
                    .plusDays(1)
                    .atStartOfDay(ZONE)
                    .minusNanos(1)
                    .toInstant();

            case THIS_YEAR -> now.with(
                            TemporalAdjusters.lastDayOfYear())
                    .toLocalDate()
                    .plusDays(1)
                    .atStartOfDay(ZONE)
                    .minusNanos(1)
                    .toInstant();

            case ALL_TIME -> Instant.now();
        };
    }

    public LocalDate getStartDate() {
        return getStart().atZone(ZONE).toLocalDate();
    }

    public LocalDate getEndDate() {
        return getEnd().atZone(ZONE).toLocalDate();
    }

    public static Instant getStart(DateRange dateRange) {
        return dateRange == null ? null : dateRange.getStart();
    }

    public static Instant getEnd(DateRange dateRange) {
        return dateRange == null ? null : dateRange.getEnd();
    }

    public static LocalDate getStartDate(DateRange dateRange) {
        return dateRange == null ? null : dateRange.getStartDate();
    }

    public static LocalDate getEndDate(DateRange dateRange) {
        return dateRange == null ? null : dateRange.getEndDate();
    }
}