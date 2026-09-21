package com.kobe.warehouse.service.dto.mobile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
     * Fin de la période de référence, <b>arrêtée au même avancement</b> que la période courante.
     *
     * <p>Une période en cours ne se compare pas à une période entière. Un 19 septembre confronté à
     * tout le mois d'août, c'est dix-neuf jours contre trente et un : la variation affichée est
     * négative d'environ quarante pour cent tous les mois, quoi que fasse l'officine, et redevient
     * juste le dernier jour du mois. On arrête donc la référence au même quantième — du 1er au 19
     * août — ce qui est la convention du cumul à date.
     *
     * <p>Quand la période courante est close, ce plafond tombe de lui-même sur la fin naturelle de la
     * période de référence : une comparaison entre deux périodes entières reste une comparaison
     * entre deux périodes entières.
     */
    public LocalDate getPreviousEndDate(LocalDate referenceDate) {
        return borneAuMemeAvancement(referenceDate, getPreviousStartDate(referenceDate));
    }

    /**
     * Début de la même période, un an plus tôt.
     *
     * <p>En officine, la comparaison au mois précédent est bruitée par la saison : épidémies
     * hivernales, paludisme des saisons humides, rentrée scolaire, congés. Un septembre se compare
     * mal à un août pour des raisons qui ne disent rien du comptoir. La comparaison à l'an passé,
     * elle, met en regard deux périodes de même nature.
     */
    public LocalDate getSamePeriodLastYearStartDate(LocalDate referenceDate) {
        return getStartDate(referenceDate).minusYears(1);
    }

    /** Fin de la même période l'an passé, arrêtée au même avancement que la période courante. */
    public LocalDate getSamePeriodLastYearEndDate(LocalDate referenceDate) {
        return borneAuMemeAvancement(referenceDate, getSamePeriodLastYearStartDate(referenceDate));
    }

    /**
     * Arrête une période de référence au même avancement que la période courante, sans jamais
     * dépasser sa propre fin naturelle.
     *
     * <p>Le plafond compte pour quelque chose : un 31 mars comparé à février doit s'arrêter le 28,
     * et non déborder sur le 2 mars.
     */
    private LocalDate borneAuMemeAvancement(LocalDate referenceDate, LocalDate debutReference) {
        LocalDate finNaturelle = switch (this) {
            case WEEK -> debutReference.plusDays(6);
            case MONTH -> debutReference.with(TemporalAdjusters.lastDayOfMonth());
            case YEAR -> debutReference.with(TemporalAdjusters.lastDayOfYear());
        };

        LocalDate memeAvancement = switch (this) {
            case WEEK -> debutReference.plusDays(ChronoUnit.DAYS.between(getStartDate(referenceDate), referenceDate));
            case MONTH -> debutReference.withDayOfMonth(Math.min(referenceDate.getDayOfMonth(), debutReference.lengthOfMonth()));
            case YEAR -> referenceDate.withYear(debutReference.getYear());
        };

        return memeAvancement.isBefore(finNaturelle) ? memeAvancement : finNaturelle;
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
