package com.kobe.warehouse.service.dto.mobile;

/**
 * DTO for chart data points used in mobile reports.
 * Represents a single data point for charts (bar, line, pie).
 */
public record ChartDataPointDTO(
    String label,
    double value,
    String color,
    String type
) {
    // Predefined colors for different chart elements
    public static final String COLOR_SALES = "#4CAF50";      // Green
    public static final String COLOR_PURCHASES = "#F44336"; // Red
    public static final String COLOR_CASH = "#2196F3";      // Blue
    public static final String COLOR_CARD = "#9C27B0";      // Purple
    public static final String COLOR_CHECK = "#FF9800";     // Orange
    public static final String COLOR_CREDIT = "#795548";    // Brown
    public static final String COLOR_MOBILE_MONEY = "#00BCD4"; // Cyan
    public static final String COLOR_TRANSFER = "#607D8B"; // Blue Grey

    // Chart types
    public static final String TYPE_SALES = "SALES";
    public static final String TYPE_PURCHASES = "PURCHASES";
    public static final String TYPE_PAYMENT = "PAYMENT";
    public static final String TYPE_VAT = "VAT";

    /**
     * Creates a sales data point.
     */
    public static ChartDataPointDTO sales(String label, double value) {
        return new ChartDataPointDTO(label, value, COLOR_SALES, TYPE_SALES);
    }

    /**
     * Creates a purchases data point.
     */
    public static ChartDataPointDTO purchases(String label, double value) {
        return new ChartDataPointDTO(label, value, COLOR_PURCHASES, TYPE_PURCHASES);
    }

    /**
     * Creates a payment data point with custom color.
     */
    public static ChartDataPointDTO payment(String label, double value, String color) {
        return new ChartDataPointDTO(label, value, color, TYPE_PAYMENT);
    }

    /**
     * Creates a VAT data point with custom color.
     */
    public static ChartDataPointDTO vat(String label, double value, String color) {
        return new ChartDataPointDTO(label, value, color, TYPE_VAT);
    }
}
