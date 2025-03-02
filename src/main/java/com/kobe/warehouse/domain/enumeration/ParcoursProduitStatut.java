package com.kobe.warehouse.domain.enumeration;

public enum ParcoursProduitStatut {
    SUGGESTION(0),
    COMMANDE_EN_COURS(1),
    COMMANDE_PASSE(2),
    ENTRE_COMMANDE(3);

    private final int value;

    ParcoursProduitStatut(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
