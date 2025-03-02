package com.kobe.warehouse.domain.enumeration;

public enum DeplacementStockType {
    MOUVEMENT_STOCK_IN("Déplacement de stock entrant"),
    MOUVEMENT_STOCK_OUT("Déplacement de stock sortant");

    private final String value;

    DeplacementStockType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
