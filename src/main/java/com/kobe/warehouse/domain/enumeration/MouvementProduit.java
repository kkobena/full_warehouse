package com.kobe.warehouse.domain.enumeration;

/**
 * The TransactionType enumeration.
 */
public enum MouvementProduit {
    SALE("Vente"),
    DELETE_SALE("Surppression de vente"),
    CANCEL_SALE("Annulation de vente"),
    AJUSTEMENT_IN("Ajustement positif"),
    AJUSTEMENT_OUT("Ajustement négatif"),
    INVENTAIRE("Inventaire"),
    COMMANDE("Commande"),
    DECONDTION_IN("Décondtion entrant"),
    DECONDTION_OUT("Décondtion sortant"),
    MOUVEMENT_STOCK_IN("Déplacement de stock entrant"),
    MOUVEMENT_STOCK_OUT("Déplacement de stock sortant"),
    ENTREE_STOCK("Entrée en stock"),
    RETRAIT_PERIME("Retrait de produit périmé"),
    RETOUR_DEPOT("Retour de dépôt"),
    RETOUR_FOURNISSEUR("Retour fournisseur");

    private final String value;

    MouvementProduit(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
