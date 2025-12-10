package com.kobe.warehouse.service.dto.mobile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Performance period enumeration for mobile reports.
 */
public enum PerformancePeriod {
    WEEK("WEEK", "Semaine", "day"),
    MONTH("MONTH", "Mois", "day"),
    YEAR("YEAR", "Année", "month");

    private final String code;
    private final String libelle;
    private final String labelFormat;

    PerformancePeriod(String code, String libelle, String labelFormat) {
        this.code = code;
        this.libelle = libelle;
        this.labelFormat = labelFormat;
    }

    public String getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getLabelFormat() {
        return labelFormat;
    }

    /**
     * Get SQL GROUP BY clause for this period.
     *
     * @return SQL expression for grouping
     */
    public String getSqlGroupBy() {
        return switch (this) {
            case WEEK, MONTH -> "s.sale_date";
            case YEAR -> "DATE_TRUNC('month', s.sale_date)::date";
        };
    }

    /**
     * Calculate period start date from reference date.
     *
     * @param referenceDate Reference date
     * @return Start date of the period
     */
    public LocalDate getStartDate(LocalDate referenceDate) {
        return switch (this) {
            case WEEK -> referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> referenceDate.withDayOfMonth(1);
            case YEAR -> referenceDate.withDayOfYear(1);
        };
    }

    /**
     * Calculate period end date from reference date.
     *
     * @param referenceDate Reference date
     * @return End date of the period
     */
    public LocalDate getEndDate(LocalDate referenceDate) {
        return switch (this) {
            case WEEK -> getStartDate(referenceDate).plusDays(6);
            case MONTH -> referenceDate.with(TemporalAdjusters.lastDayOfMonth());
            case YEAR -> referenceDate.with(TemporalAdjusters.lastDayOfYear());
        };
    }

    /**
     * Calculate previous period start date.
     *
     * @param referenceDate Reference date
     * @return Start date of the previous period
     */
    public LocalDate getPreviousStartDate(LocalDate referenceDate) {
        LocalDate startDate = getStartDate(referenceDate);
        return switch (this) {
            case WEEK -> startDate.minusWeeks(1);
            case MONTH -> startDate.minusMonths(1);
            case YEAR -> startDate.minusYears(1);
        };
    }

    /**
     * Calculate previous period end date.
     *
     * @param referenceDate Reference date
     * @return End date of the previous period
     */
    public LocalDate getPreviousEndDate(LocalDate referenceDate) {
        LocalDate previousStartDate = getPreviousStartDate(referenceDate);
        return switch (this) {
            case WEEK -> previousStartDate.plusDays(6);
            case MONTH -> previousStartDate.with(TemporalAdjusters.lastDayOfMonth());
            case YEAR -> previousStartDate.with(TemporalAdjusters.lastDayOfYear());
        };
    }

    /**
     * Parse period from string.
     *
     * @param period Period string
     * @return PerformancePeriod enum value
     * @throws IllegalArgumentException if period is invalid
     */
    public static PerformancePeriod fromString(String period) {
        if (period == null || period.isBlank()) {
            throw new IllegalArgumentException("Period cannot be null or empty");
        }
        return switch (period.toUpperCase()) {
            case "WEEK" -> WEEK;
            case "MONTH" -> MONTH;
            case "YEAR" -> YEAR;
            default -> throw new IllegalArgumentException("Invalid period: " + period + ". Expected: WEEK, MONTH, or YEAR");
        };
    }
}
