package com.kobe.warehouse.service.dto.enumeration;

import lombok.Getter;

@Getter
public enum TypeVenteDTO {
  CashSale("VNO"),
  ThirdPartySales("VO");
  private final String value;

  TypeVenteDTO(String value) {
    this.value = value;
  }
}
