package com.kobe.warehouse.domain.enumeration;

public enum TypeFinancialTransaction {
  CASH_SALE("VNO", CategorieTransaction.VENTES),
  CREDIT_SALE("VO", CategorieTransaction.VENTES),
  VENTES_DEPOTS("Ventes dépôts", CategorieTransaction.VENTES),
  VENTES_DEPOTS_AGREE("VO", CategorieTransaction.VENTES),
  REGLEMENT_DIFFERE("Règlements différés", CategorieTransaction.ENTREE),
  REGLEMENT_TIERS_PAYANT("Règlements tiers payant", CategorieTransaction.ENTREE),
  SORTIE_CAISSE("Sortie de caisse", CategorieTransaction.SORTIE_CAISSE),
  ENTREE_CAISSE("Entrée de caisse", CategorieTransaction.ENTREE),
  FONDS_CAISSE("Fonds de caisse", CategorieTransaction.SORTIE_CAISSE),
  REGLMENT_FOURNISSEUR("Règlement facture fournisseur", CategorieTransaction.SORTIE_CAISSE);

  private final String value;
  private final CategorieTransaction categorieTransaction;

  TypeFinancialTransaction(String value, CategorieTransaction categorieTransaction) {
    this.value = value;
    this.categorieTransaction = categorieTransaction;
  }

  public CategorieTransaction getCategorieTransaction() {
    return categorieTransaction;
  }

  public String getValue() {
    return value;
  }
}
