package com.kobe.warehouse.service.cash_register.dto;

public enum TypeVente {
  CASH_SALE("CashSale"),
  CREDIT_SALE("ThirdPartySales"),
  VENTES_DEPOTS("VenteDepot"),
  VENTES_DEPOT_AGREE("VenteDepotAgree");
  private final String value;

  TypeVente(String value) {
    this.value = value;
  }

  public static TypeVente fromValue(String value) {
    for (TypeVente typeVente : TypeVente.values()) {
      if (typeVente.value.equals(value)) {
        return typeVente;
      }
    }
    return null;
  }

  public String getValue() {
    return value;
  }
}
