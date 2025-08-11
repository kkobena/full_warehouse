package com.kobe.warehouse.domain.enumeration;

/**
 * The TransactionType enumeration.
 */
public enum TransactionType {
    SALE("Vente", TransactionTypeGroup.VENTE),
    DELETE_SALE("Surppression de vente", TransactionTypeGroup.VENTE),
    CANCEL_SALE("Annulation de vente", TransactionTypeGroup.VENTE),
    REAPPRO("Réapprovisionnement", TransactionTypeGroup.PRODUIT),
    AJUSTEMENT_IN("Ajustement positif", TransactionTypeGroup.PRODUIT),
    AJUSTEMENT_OUT("Ajustement négatif", TransactionTypeGroup.PRODUIT),
    INVENTAIRE("Inventaire", TransactionTypeGroup.PRODUIT),
    SUPPRESSION("Suppression", TransactionTypeGroup.PRODUIT),
    COMMANDE("Commande", TransactionTypeGroup.COMMANDE),
    DECONDTION_IN("Décondtion entrant", TransactionTypeGroup.PRODUIT),
    DECONDTION_OUT("Décondtion sortant", TransactionTypeGroup.PRODUIT),
    CREATE_PRODUCT("Création de nouveau produit", TransactionTypeGroup.PRODUIT),
    UPDATE_PRODUCT("Modification info  produit", TransactionTypeGroup.PRODUIT),
    DELETE_PRODUCT("Supression  produit", TransactionTypeGroup.PRODUIT),
    DISABLE_PRODUCT("Désactivation  produit", TransactionTypeGroup.PRODUIT),
    ENABLE_PRODUCT("Activation  produit", TransactionTypeGroup.PRODUIT),
    MODIFICATION_PRIX_PRODUCT("Modification prix  produit", TransactionTypeGroup.PRODUIT),
    MOUVEMENT_STOCK_IN("Déplacement de stock entrant", TransactionTypeGroup.PRODUIT),
    MOUVEMENT_STOCK_OUT("Déplacement de stock sortant", TransactionTypeGroup.PRODUIT),
    FORCE_STOCK("Vente en avoir", TransactionTypeGroup.VENTE),
    MODIFICATION_PRIX_PRODUCT_A_LA_VENTE("Modification prix  produit à la vente", TransactionTypeGroup.VENTE),
    ENTREE_STOCK("Entrée en stock", TransactionTypeGroup.COMMANDE),
    ACTIVATION_PRIVILEGE("Utilisation de la clé d'activation d'une action", TransactionTypeGroup.PRIVILEGE),
    RETRAIT_PERIME("Retrait de produit périmé", TransactionTypeGroup.PRODUIT),
    MODIFICATION_DATE_DE_VENTE("Modification de la de vente", TransactionTypeGroup.VENTE),
    MODIFICATION_INFO_CLIENT("Modification des informations du client", TransactionTypeGroup.VENTE);

    private final String value;
    private final TransactionTypeGroup transactionTypeGroup;

    TransactionType(String value, TransactionTypeGroup transactionTypeGroup) {
        this.value = value;
        this.transactionTypeGroup = transactionTypeGroup;
    }

    public TransactionTypeGroup getTransactionTypeGroup() {
        return transactionTypeGroup;
    }

    public String getValue() {
        return value;
    }
}
