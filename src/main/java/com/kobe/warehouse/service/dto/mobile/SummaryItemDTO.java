package com.kobe.warehouse.service.dto.mobile;

/**
 * DTO for individual summary items in cash summary report.
 *
 * @param key      Unique key for the item (e.g., "ESPECES", "CARTES")
 * @param libelle  Display label
 * @param value    Primary value
 * @param secondValue Secondary value (if applicable)
 * @param type     Item type (AMOUNT, COUNT, PERCENT)
 */
public record SummaryItemDTO(
    String key,
    String libelle,
    long value,
    long secondValue,
    String type
) {
    public static final String TYPE_AMOUNT = "AMOUNT";
    public static final String TYPE_COUNT = "COUNT";
    public static final String TYPE_PERCENT = "PERCENT";

    public SummaryItemDTO(String key, String libelle, long value, String type) {
        this(key, libelle, value, value, type);
    }

    public SummaryItemDTO(String libelle, long value, long secondValue) {
        this(libelle.toUpperCase().replace(" ", "_"), libelle, value, secondValue, TYPE_AMOUNT);
    }
}
