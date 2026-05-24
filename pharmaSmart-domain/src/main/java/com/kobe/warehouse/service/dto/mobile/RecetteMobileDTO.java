package com.kobe.warehouse.service.dto.mobile;

/**
 * DTO for receipt by payment mode in mobile activity report.
 *
 * @param code     Payment mode code (e.g., CASH, CB, OM)
 * @param libelle  Payment mode label
 * @param montant  Amount received
 * @param percent  Percentage of total
 * @param color    Color for chart display
 */
public record RecetteMobileDTO(
    String code,
    String libelle,
    long montant,
    double percent,
    String color
) {
    // Colors for different payment modes
    private static final String COLOR_CASH = "#28A745";      // Green for cash
    private static final String COLOR_CARD = "#007BFF";      // Blue for cards
    private static final String COLOR_MOBILE = "#FFC107";    // Yellow for mobile
    private static final String COLOR_CHECK = "#6C757D";     // Gray for checks
    private static final String COLOR_TRANSFER = "#17A2B8";  // Cyan for transfers
    private static final String COLOR_DEFAULT = "#6C757D";   // Gray default

    public static String getColorForCode(String code) {
        if (code == null) return COLOR_DEFAULT;
        return switch (code.toUpperCase()) {
            case "CASH" -> COLOR_CASH;
            case "CB" -> COLOR_CARD;
            case "OM", "MTN", "MOOV", "WAVE" -> COLOR_MOBILE;
            case "CH" -> COLOR_CHECK;
            case "VIREMENT" -> COLOR_TRANSFER;
            default -> COLOR_DEFAULT;
        };
    }
}
