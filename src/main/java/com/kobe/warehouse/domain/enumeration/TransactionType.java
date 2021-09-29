package com.kobe.warehouse.domain.enumeration;

/**
 * The TransactionType enumeration.
 */
public enum TransactionType {
    SALE("Vente"),
    DELETE_SALE("Surppression de vente"),
    CANCEL_SALE("Annulation de vente"),
    REAPPRO("Réapprovisionnement"),
    AJUSTEMENT_IN("Ajustement positif"),
    AJUSTEMENT_OUT("Ajustement négatif"),
    INVENTAIRE("Inventaire"),
    SUPPRESSION("Suppression"),
    COMMANDE("Commande"),
    DECONDTION_IN("Décondtion entrant"),
    DECONDTION_OUT("Décondtion sortant"),
    CREATE_PRODUCT("Création de nouveau produit"),
    UPDATE_PRODUCT("Modification info  produit"),
    DELETE_PRODUCT("Supression  produit"),
    DISABLE_PRODUCT("Désactivation  produit"),
    ENABLE_PRODUCT("Activation  produit"),
    MODIFICATION_PRIX_PRODUCT("Modification prix  produit")
    ;

    private final String value;


    TransactionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
