package com.kobe.warehouse.service.dashboard.widget;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.Map;

/**
 * Données d'un widget, dans une forme indépendante de la visualisation : la même série s'affiche
 * en courbe, en barres ou en tableau.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = WidgetData.Kpi.class, name = "KPI"),
        @JsonSubTypes.Type(value = WidgetData.Series.class, name = "SERIES"),
        @JsonSubTypes.Type(value = WidgetData.Table.class, name = "TABLE"),
    }
)
public sealed interface WidgetData {
    /**
     * Une valeur, et la valeur de comparaison pour calculer l'évolution ({@code null} si elle n'a
     * pas de sens). {@code detail} : ligne de contexte sous la valeur. {@code previousLabel} : à quoi
     * la valeur est comparée ; nul, c'est la période précédente de même durée.
     */
    record Kpi(Number value, Number previousValue, String unit, String detail, String previousLabel) implements WidgetData {
        public Kpi(Number value, Number previousValue, String unit, String detail) {
            this(value, previousValue, unit, detail, null);
        }
    }

    record Series(List<String> labels, List<SeriesItem> series, String footer) implements WidgetData {}

    record SeriesItem(String name, List<? extends Number> values) {}

    record Table(List<Column> columns, List<Map<String, Object>> rows, String footer) implements WidgetData {}

    /** {@code type} : string, number, amount, percent, date, datetime. */
    record Column(String field, String header, String type) {
        public static Column text(String field, String header) {
            return new Column(field, header, "string");
        }

        public static Column number(String field, String header) {
            return new Column(field, header, "number");
        }

        public static Column amount(String field, String header) {
            return new Column(field, header, "amount");
        }
    }
}
