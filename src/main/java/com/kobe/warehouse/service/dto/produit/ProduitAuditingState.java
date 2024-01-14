package com.kobe.warehouse.service.dto.produit;

import java.time.LocalDate;

public class ProduitAuditingState {
  private LocalDate mvtDate;
  private Integer initStock;
  private Integer saleQuantity;
  private Integer deleveryQuantity;
  private Integer retourFournisseurQuantity;
  private Integer perimeQuantity;
  private Integer ajustementPositifQuantity;
  private Integer ajustementNegatifQuantity;
  private Integer deconPositifQuantity;
  private Integer deconNegatifQuantity;
  private Integer canceledQuantity;
  private Integer retourDepot;
  private Integer storeInventoryQuantity;
  private Integer inventoryGap;
  private Integer afterStock;
  private String transactionDate;

  public String getTransactionDate() {
    return transactionDate;
  }

  public ProduitAuditingState setTransactionDate(String transactionDate) {
    this.transactionDate = transactionDate;
    return this;
  }

  public LocalDate getMvtDate() {
    return mvtDate;
  }

  public ProduitAuditingState setMvtDate(LocalDate mvtDate) {
    this.mvtDate = mvtDate;
    return this;
  }

  public Integer getInitStock() {
    return initStock;
  }

  public ProduitAuditingState setInitStock(Integer initStock) {
    this.initStock = initStock;
    return this;
  }

  public Integer getSaleQuantity() {
    return saleQuantity;
  }

  public ProduitAuditingState setSaleQuantity(Integer saleQuantity) {
    this.saleQuantity = saleQuantity;
    return this;
  }

  public Integer getDeleveryQuantity() {
    return deleveryQuantity;
  }

  public ProduitAuditingState setDeleveryQuantity(Integer deleveryQuantity) {
    this.deleveryQuantity = deleveryQuantity;
    return this;
  }

  public Integer getRetourFournisseurQuantity() {
    return retourFournisseurQuantity;
  }

  public ProduitAuditingState setRetourFournisseurQuantity(Integer retourFournisseurQuantity) {
    this.retourFournisseurQuantity = retourFournisseurQuantity;
    return this;
  }

  public Integer getPerimeQuantity() {
    return perimeQuantity;
  }

  public ProduitAuditingState setPerimeQuantity(Integer perimeQuantity) {
    this.perimeQuantity = perimeQuantity;
    return this;
  }

  public Integer getAjustementPositifQuantity() {
    return ajustementPositifQuantity;
  }

  public ProduitAuditingState setAjustementPositifQuantity(Integer ajustementPositifQuantity) {
    this.ajustementPositifQuantity = ajustementPositifQuantity;
    return this;
  }

  public Integer getAjustementNegatifQuantity() {
    return ajustementNegatifQuantity;
  }

  public ProduitAuditingState setAjustementNegatifQuantity(Integer ajustementNegatifQuantity) {
    this.ajustementNegatifQuantity = ajustementNegatifQuantity;
    return this;
  }

  public Integer getDeconPositifQuantity() {
    return deconPositifQuantity;
  }

  public ProduitAuditingState setDeconPositifQuantity(Integer deconPositifQuantity) {
    this.deconPositifQuantity = deconPositifQuantity;
    return this;
  }

  public Integer getDeconNegatifQuantity() {
    return deconNegatifQuantity;
  }

  public ProduitAuditingState setDeconNegatifQuantity(Integer deconNegatifQuantity) {
    this.deconNegatifQuantity = deconNegatifQuantity;
    return this;
  }

  public Integer getCanceledQuantity() {
    return canceledQuantity;
  }

  public ProduitAuditingState setCanceledQuantity(Integer canceledQuantity) {
    this.canceledQuantity = canceledQuantity;
    return this;
  }

  public Integer getRetourDepot() {
    return retourDepot;
  }

  public ProduitAuditingState setRetourDepot(Integer retourDepot) {
    this.retourDepot = retourDepot;
    return this;
  }

  public Integer getStoreInventoryQuantity() {
    return storeInventoryQuantity;
  }

  public ProduitAuditingState setStoreInventoryQuantity(Integer storeInventoryQuantity) {
    this.storeInventoryQuantity = storeInventoryQuantity;
    return this;
  }

  public Integer getInventoryGap() {
    return inventoryGap;
  }

  public ProduitAuditingState setInventoryGap(Integer inventoryGap) {
    this.inventoryGap = inventoryGap;
    return this;
  }

  public Integer getAfterStock() {
    return afterStock;
  }

  public ProduitAuditingState setAfterStock(Integer afterStock) {
    this.afterStock = afterStock;
    return this;
  }
}
