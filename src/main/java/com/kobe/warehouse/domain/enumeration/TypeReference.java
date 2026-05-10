package com.kobe.warehouse.domain.enumeration;

public enum TypeReference {
    VENTE(0),
    COMMANDE(1),
    PREVENTE_VENTE(2),
    SUGGESTION(3),
    TRANSACTION(4),
    REASSORT(5),
    AVOIR_CLIENT(6),
    RETOUR_CLIENT(7);
    private final int value;

    TypeReference(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
