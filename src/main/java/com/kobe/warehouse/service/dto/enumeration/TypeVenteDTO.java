package com.kobe.warehouse.service.dto.enumeration;

public enum TypeVenteDTO {
  CashSale("VNO"),
  ThirdPartySales("VO");
  private final String value;

  TypeVenteDTO(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
