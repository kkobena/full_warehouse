package com.kobe.warehouse.service.dto.enumeration;

public enum InventoryExportSummaryEnum {
    ACHAT_AVANT("Valeur Achat Avant"),
    ACHAT_APRES("Valeur Achat Après"),
    VENTE_APRES("Valeur Vente Après"),
    VENTE_AVANT("Valeur Vente Avant"),
    ACHAT_ECART("Achat écart"),
    VENTE_ECART("Vente écart");

    private final String value;

    InventoryExportSummaryEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
