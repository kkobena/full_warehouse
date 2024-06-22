package com.kobe.warehouse.service.dto.filter;

public class InventoryTransactionFilterDTO {
  private Long produitId;
  private String startDate;
  private String endDate;
  private Integer type;

  public Long getProduitId() {
    return produitId;
  }

  public InventoryTransactionFilterDTO setProduitId(Long produitId) {
    this.produitId = produitId;
    return this;
  }

  public String getStartDate() {
    return startDate;
  }

  public InventoryTransactionFilterDTO setStartDate(String startDate) {
    this.startDate = startDate;
    return this;
  }

  public String getEndDate() {
    return endDate;
  }

  public InventoryTransactionFilterDTO setEndDate(String endDate) {
    this.endDate = endDate;
    return this;
  }

  public Integer getType() {
    return type;
  }

  public InventoryTransactionFilterDTO setType(Integer type) {
    this.type = type;
    return this;
  }
}
