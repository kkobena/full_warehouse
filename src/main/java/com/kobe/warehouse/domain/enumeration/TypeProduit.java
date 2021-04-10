package com.kobe.warehouse.domain.enumeration;

/**
 * The TypeProduit enumeration.
 */
public enum TypeProduit {
    DETAIL("Détail"),
    PACKAGE("Carton");

    private final String value;


    TypeProduit(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
