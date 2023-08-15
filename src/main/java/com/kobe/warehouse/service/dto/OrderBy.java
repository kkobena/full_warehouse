package com.kobe.warehouse.service.dto;

import lombok.Getter;

@Getter
public enum OrderBy {
  QUANTITY_SOLD("quantity_sold"),
  AMOUNT("sales_amount");

  private final String value;

  OrderBy(String value) {
    this.value = value;
  }
}
