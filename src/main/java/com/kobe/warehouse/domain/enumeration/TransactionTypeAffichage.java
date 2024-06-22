package com.kobe.warehouse.domain.enumeration;

public enum TransactionTypeAffichage {
  VNO("VNO"),
  VO("VO"),
  VENTES_DEPOTS("Ventes dépôts"),
  ENTREE_CAISSE("Entrée de caisse"),
  SORTIE_CAISSE("Sortie de caisse"),
  REGLEMENT_DIFFERE("Règlements différés"),
  REGLEMENT_TIERS_PAYANT("Règlements tiers payant"),
  REGLEMENT_FOURNISSEUR("Règlement facture fournisseur");
  private final String value;

  TransactionTypeAffichage(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
