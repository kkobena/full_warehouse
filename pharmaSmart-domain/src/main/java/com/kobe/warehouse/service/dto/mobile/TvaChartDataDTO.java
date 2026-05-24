package com.kobe.warehouse.service.dto.mobile;

/**
 * DTO for TVA chart data point.
 */
public record TvaChartDataDTO(
    String label,    // e.g., "0%", "18%", "20%"
    long value,
    double percent,
    String color
) {
    private static final String[] COLORS = {
        "#4CAF50",  // Green
        "#2196F3",  // Blue
        "#FF9800",  // Orange
        "#9C27B0",  // Purple
        "#F44336",  // Red
        "#00BCD4",  // Cyan
        "#795548",  // Brown
        "#607D8B"   // Blue Grey
    };

    public static String getColor(int index) {
        return COLORS[index % COLORS.length];
    }
}
