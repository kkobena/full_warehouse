package com.kobe.warehouse.domain.enumeration;

public enum CategorieTransaction {
    VENTES("Ventes"),
    SORTIE_CAISSE("Sortie de caisse"),
    ENTREE("Entrée de caisse");

    private final String value;

    CategorieTransaction(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
