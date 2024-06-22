package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import java.time.LocalDateTime;

public class StockProduitDTO {

  private Long id;
  private Integer qtyStock;
  private int qtyVirtual;
  private int qtyUG;
  private Long storageId;
  private String storageName;
  private String storageType;
  private Long produitId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String produitLibelle;

  public StockProduitDTO(StockProduit s) {
    this.id = s.getId();
    this.qtyStock = s.getQtyStock();
    this.qtyVirtual = s.getQtyVirtual();
    this.qtyUG = s.getQtyUG();
    Storage r = s.getStorage();
    this.storageId = r.getId();
    this.storageName = r.getName();
    this.storageType = r.getStorageType().getValue();
    Produit p = s.getProduit();
    this.produitId = p.getId();
    this.createdAt = p.getCreatedAt();
    this.updatedAt = p.getUpdatedAt();
    this.produitLibelle = p.getLibelle();
  }

  public StockProduitDTO() {}

  public String getProduitLibelle() {
    return produitLibelle;
  }

  public StockProduitDTO setProduitLibelle(String produitLibelle) {
    this.produitLibelle = produitLibelle;
    return this;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public StockProduitDTO setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public StockProduitDTO setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Long getProduitId() {
    return produitId;
  }

  public StockProduitDTO setProduitId(Long produitId) {
    this.produitId = produitId;
    return this;
  }

  public String getStorageType() {
    return storageType;
  }

  public StockProduitDTO setStorageType(String storageType) {
    this.storageType = storageType;
    return this;
  }

  public String getStorageName() {
    return storageName;
  }

  public StockProduitDTO setStorageName(String storageName) {
    this.storageName = storageName;
    return this;
  }

  public Long getStorageId() {
    return storageId;
  }

  public StockProduitDTO setStorageId(Long storageId) {
    this.storageId = storageId;
    return this;
  }

  public int getQtyUG() {
    return qtyUG;
  }

  public StockProduitDTO setQtyUG(int qtyUG) {
    this.qtyUG = qtyUG;
    return this;
  }

  public int getQtyVirtual() {
    return qtyVirtual;
  }

  public StockProduitDTO setQtyVirtual(int qtyVirtual) {
    this.qtyVirtual = qtyVirtual;
    return this;
  }

  public Integer getQtyStock() {
    return qtyStock;
  }

  public StockProduitDTO setQtyStock(Integer qtyStock) {
    this.qtyStock = qtyStock;
    return this;
  }

  public Long getId() {
    return id;
  }

  public StockProduitDTO setId(Long id) {
    this.id = id;
    return this;
  }
}
