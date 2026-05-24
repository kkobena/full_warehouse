package com.kobe.warehouse.service.dto.mobile;

/**
 * Alert type enumeration for mobile alerts.
 */
public enum AlertType {
    STOCK_RUPTURE("STOCK_RUPTURE", "Rupture de stock", "alert-circle"),
    STOCK_LOW("STOCK_LOW", "Stock faible", "alert-triangle"),
    EXPIRY("EXPIRY", "Péremption proche", "clock-alert"),
    CASH_DISCREPANCY("CASH_DISCREPANCY", "Écart de caisse", "cash-register"),
    INVOICE_OVERDUE("INVOICE_OVERDUE", "Facture impayée", "file-document-alert");

    private final String code;
    private final String libelle;
    private final String icon;

    AlertType(String code, String libelle, String icon) {
        this.code = code;
        this.libelle = libelle;
        this.icon = icon;
    }

    public String getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getIcon() {
        return icon;
    }
}
