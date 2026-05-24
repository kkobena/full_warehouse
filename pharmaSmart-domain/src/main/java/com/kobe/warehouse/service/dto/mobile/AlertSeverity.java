package com.kobe.warehouse.service.dto.mobile;

/**
 * Alert severity enumeration for mobile alerts.
 */
public enum AlertSeverity {
    CRITICAL("CRITICAL", "Critique", "#991B1B"),
    WARNING("WARNING", "Attention", "#FFC107"),
    INFO("INFO", "Information", "#17A2B8");

    private final String code;
    private final String libelle;
    private final String color;

    AlertSeverity(String code, String libelle, String color) {
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
     * Get color variant for critical threshold.
     *
     * @param isCritical Whether the threshold is critical
     * @return CRITICAL if true, WARNING otherwise
     */
    public static AlertSeverity fromCriticalThreshold(boolean isCritical) {
        return isCritical ? CRITICAL : WARNING;
    }
}
