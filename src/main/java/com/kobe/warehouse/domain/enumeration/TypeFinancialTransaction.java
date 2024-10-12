package com.kobe.warehouse.domain.enumeration;

public enum TypeFinancialTransaction {
    CASH_SALE("VNO", CategorieTransaction.VENTES, TransactionTypeAffichage.VNO),
    CREDIT_SALE("VO", CategorieTransaction.VENTES, TransactionTypeAffichage.VO),
    VENTES_DEPOTS("Ventes dépôts", CategorieTransaction.VENTES, TransactionTypeAffichage.VENTES_DEPOTS),
    VENTES_DEPOTS_AGREE("VO", CategorieTransaction.VENTES, TransactionTypeAffichage.VO),
    REGLEMENT_DIFFERE("Règlements différés", CategorieTransaction.ENTREE, TransactionTypeAffichage.REGLEMENT_DIFFERE),
    REGLEMENT_TIERS_PAYANT("Règlements tiers payant", CategorieTransaction.ENTREE, TransactionTypeAffichage.REGLEMENT_TIERS_PAYANT),
    SORTIE_CAISSE("Sortie de caisse", CategorieTransaction.SORTIE_CAISSE, TransactionTypeAffichage.SORTIE_CAISSE),
    ENTREE_CAISSE("Entrée de caisse", CategorieTransaction.ENTREE, TransactionTypeAffichage.ENTREE_CAISSE),
    FONDS_CAISSE("Fonds de caisse", CategorieTransaction.SORTIE_CAISSE, TransactionTypeAffichage.SORTIE_CAISSE),
    REGLMENT_FOURNISSEUR(
        "Règlement facture fournisseur",
        CategorieTransaction.SORTIE_CAISSE,
        TransactionTypeAffichage.REGLEMENT_FOURNISSEUR
    );

    private final String value;
    private final CategorieTransaction categorieTransaction;
    private final TransactionTypeAffichage transactionTypeAffichage;

    TypeFinancialTransaction(String value, CategorieTransaction categorieTransaction, TransactionTypeAffichage transactionTypeAffichage) {
        this.value = value;
        this.categorieTransaction = categorieTransaction;
        this.transactionTypeAffichage = transactionTypeAffichage;
    }

    public TransactionTypeAffichage getTransactionTypeAffichage() {
        return transactionTypeAffichage;
    }

    public CategorieTransaction getCategorieTransaction() {
        return categorieTransaction;
    }

    public String getValue() {
        return value;
    }
}
