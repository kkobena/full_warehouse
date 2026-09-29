package com.kobe.warehouse.service.dashboard.widget;

import com.kobe.warehouse.service.dto.VenteRecordParamDTO;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/** Période d'un widget et paramètres numériques bornés. */
public final class WidgetPeriods {

    private WidgetPeriods() {}

    /** Bornes de la période ; sans dates, la journée en cours. */
    public record Period(LocalDate start, LocalDate end) {
        public long days() {
            return ChronoUnit.DAYS.between(start, end) + 1;
        }

        /** Fenêtre de même durée, juste avant : sert à calculer l'évolution. */
        public Period previous() {
            LocalDate previousEnd = start.minusDays(1);
            return new Period(previousEnd.minusDays(days() - 1), previousEnd);
        }

        /**
         * Paramètre des services de statistiques de ventes. La période {@code daily} (valeur par
         * défaut) est celle qui respecte les dates reçues ; les autres les recalculent.
         */
        public VenteRecordParamDTO toVenteParam() {
            return new VenteRecordParamDTO().setFromDate(start).setToDate(end);
        }

        /** Granularité d'une courbe lisible : jour sur un mois, semaine sur un semestre, mois au-delà. */
        public String evolutionGranularity() {
            long days = days();
            return days <= 31 ? "daily" : days <= 183 ? "weekly" : "monthly";
        }
    }

    public static Period of(WidgetContext context) {
        LocalDate today = LocalDate.now();
        LocalDate start = context.startDate() != null ? context.startDate() : today;
        LocalDate end = context.endDate() != null ? context.endDate() : start;
        return end.isBefore(start) ? new Period(end, start) : new Period(start, end);
    }

    /** Paramètre entier du widget, ramené dans [min, max] ; la valeur par défaut si absent ou invalide. */
    public static int intParam(Map<String, String> params, String name, int defaultValue, int min, int max) {
        String raw = params == null ? null : params.get(name);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Math.clamp(Integer.parseInt(raw.trim()), min, max);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
