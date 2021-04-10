package com.kobe.warehouse.domain.enumeration;

/**
 * The TransactionType enumeration.
 */
public enum TransactionType {
    SALE("Vente"),
    REAPPRO("Réapprovisionnement"),
    AJUSTEMENT_IN("Ajustement positif"),
    AJUSTEMENT_OUT("Ajustement négatif"),
    INVENTAIRE("Inventaire"),
    SUPPRESSION("Suppression"),
    COMMANDE("Commande"),
    DECONDTION_IN("Décondtion entrant"),
    DECONDTION_OUT("Décondtion sortant");

    private final String value;


    TransactionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
