package com.kobe.warehouse.service.dto.mobile;

/**
 * Stock status enumeration for mobile product info.
 */
public enum StockStatus {
    RUPTURE("RUPTURE", "Rupture de stock", "red"),
    LOW("LOW", "Stock faible", "orange"),
    OK("OK", "Stock suffisant", "green");

    private final String code;
    private final String libelle;
    private final String color;

    StockStatus(String code, String libelle, String color) {
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
     * Determine stock status based on quantity and threshold.
     *
     * @param totalQuantity Total stock quantity
     * @param minThreshold  Minimum threshold
     * @return Appropriate stock status
     */
    public static StockStatus fromQuantity(int totalQuantity, int minThreshold) {
        if (totalQuantity == 0) {
            return RUPTURE;
        } else if (totalQuantity <= minThreshold) {
            return LOW;
        } else {
            return OK;
        }
    }
}
