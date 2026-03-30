package com.github.fiodarks.project26.archive.domain.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public final class PartialDate {
    public enum Precision {
        YEAR,
        MONTH,
        DAY
    }

    private final String raw;
    private final Precision precision;
    private final int year;
    private final Integer month;
    private final Integer day;

    private PartialDate(String raw, Precision precision, int year, Integer month, Integer day) {
        this.raw = Objects.requireNonNull(raw, "raw");
        this.precision = Objects.requireNonNull(precision, "precision");
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public static PartialDate parse(String value) {
        Objects.requireNonNull(value, "value");
        try {
            if (value.matches("^\\d{4}$")) {
                int year = Integer.parseInt(value);
                return new PartialDate(value, Precision.YEAR, year, null, null);
            }
            if (value.matches("^\\d{4}-\\d{2}$")) {
                var ym = YearMonth.parse(value);
                return new PartialDate(value, Precision.MONTH, ym.getYear(), ym.getMonthValue(), null);
            }
            if (value.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                var date = LocalDate.parse(value);
                return new PartialDate(value, Precision.DAY, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
            }
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new IllegalArgumentException("Invalid date value: " + value, e);
        }
        throw new IllegalArgumentException("Invalid date value: " + value);
    }

    public String raw() {
        return raw;
    }

    public Precision precision() {
        return precision;
    }

    public LocalDate lowerBoundInclusive() {
        return switch (precision) {
            case YEAR -> LocalDate.of(year, 1, 1);
            case MONTH -> LocalDate.of(year, month, 1);
            case DAY -> LocalDate.of(year, month, day);
        };
    }

    public LocalDate upperBoundInclusive() {
        return switch (precision) {
            case YEAR -> LocalDate.of(year, 12, 31);
            case MONTH -> YearMonth.of(year, month).atEndOfMonth();
            case DAY -> LocalDate.of(year, month, day);
        };
    }
}
