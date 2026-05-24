package com.kobe.warehouse.service.dto.mobile;

/**
 * DTO for payment mode breakdown in cash balance report.
 */
public record PaymentModeBreakdownDTO(
    String code,
    String libelle,
    long montant,
    double percent,
    String color
) {
    public static String getColorForMode(String code) {
        return switch (code) {
            case "CASH", "ESPECES" -> "#4CAF50";      // Green
            case "CB", "CARTE" -> "#2196F3";          // Blue
            case "CHEQUE" -> "#FF9800";               // Orange
            case "VIREMENT" -> "#9C27B0";             // Purple
            case "MOBILE_MONEY", "OM", "WAVE" -> "#00BCD4"; // Cyan
            case "CREDIT" -> "#F44336";               // Red
            case "DIFFERE" -> "#795548";              // Brown
            case "TIERS_PAYANT" -> "#607D8B";         // Blue Grey
            default -> "#9E9E9E";                     // Grey
        };
    }
}
