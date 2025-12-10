package com.kobe.warehouse.service.dto.mobile;

/**
 * Expiry status enumeration for lot/batch information.
 */
public enum ExpiryStatus {
    EXPIRED("EXPIRED", "Expiré", "red"),
    CRITICAL("CRITICAL", "Critique (< 30 jours)", "red"),
    WARNING("WARNING", "Attention (< 90 jours)", "orange"),
    OK("OK", "Valide", "green");

    private final String code;
    private final String libelle;
    private final String color;

    ExpiryStatus(String code, String libelle, String color) {
        this.code = code;
        this.libelle = libelle;
        this.color = color;
    }

    public String getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getColor() {
        return color;
    }

    /**
     * Determine expiry status based on days until expiry.
     *
     * @param daysUntilExpiry Number of days until expiration
     * @return Appropriate expiry status
     */
    public static ExpiryStatus fromDaysUntilExpiry(int daysUntilExpiry) {
        if (daysUntilExpiry <= 0) {
            return EXPIRED;
        } else if (daysUntilExpiry <= 30) {
            return CRITICAL;
        } else if (daysUntilExpiry <= 90) {
            return WARNING;
        } else {
            return OK;
        }
    }
}
