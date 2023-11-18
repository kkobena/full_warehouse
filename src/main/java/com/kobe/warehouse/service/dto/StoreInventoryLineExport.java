package com.kobe.warehouse.service.dto;

import java.math.BigInteger;

public class StoreInventoryLineExport {
  private Integer gap;
  private Integer inventoryValueCost;
  private Integer quantityInit;
  private Integer quantityOnHand;
  private String produitCip;
  private String produitEan;
  private String produitLibelle;
  private String rayonLibelle;
  private String storageLibelle;
  private Integer prixAchat;
  private Integer prixUni;
  private Integer lastUnitPrice;
  private BigInteger rayonId;
  private BigInteger storageId;
  private String rayonCode;
  private String famillyCode;
  private String famillyLibelle;
  private Integer famillyId;

  public StoreInventoryLineExport(
      Integer gap,
      Integer inventoryValueCost,
      Integer quantityInit,
      Integer quantityOnHand,
      String produitCip,
      String produitEan,
      String produitLibelle,
      String rayonLibelle,
      String storageLibelle,
      Integer prixAchat,
      Integer prixUni,
      Integer lastUnitPrice,
      BigInteger rayonId,
      BigInteger storageId,
      String rayonCode,
      String famillyCode,
      String famillyLibelle,
      Integer famillyId) {
    this.gap = gap;
    this.inventoryValueCost = inventoryValueCost;
    this.quantityInit = quantityInit;
    this.quantityOnHand = quantityOnHand;
    this.produitCip = produitCip;
    this.produitEan = produitEan;
    this.produitLibelle = produitLibelle;
    this.rayonLibelle = rayonLibelle;
    this.storageLibelle = storageLibelle;
    this.prixAchat = prixAchat;
    this.prixUni = prixUni;
    this.lastUnitPrice = lastUnitPrice;
    this.rayonId = rayonId;
    this.storageId = storageId;
    this.rayonCode = rayonCode;
    this.famillyCode = famillyCode;
    this.famillyLibelle = famillyLibelle;
    this.famillyId = famillyId;
  }

  public Integer getFamillyId() {
    return famillyId;
  }

  public StoreInventoryLineExport setFamillyId(Integer famillyId) {
    this.famillyId = famillyId;
    return this;
  }

  public String getFamillyCode() {
    return famillyCode;
  }

  public StoreInventoryLineExport setFamillyCode(String famillyCode) {
    this.famillyCode = famillyCode;
    return this;
  }

  public String getFamillyLibelle() {
    return famillyLibelle;
  }

  public StoreInventoryLineExport setFamillyLibelle(String famillyLibelle) {
    this.famillyLibelle = famillyLibelle;
    return this;
  }

  public Integer getGap() {
    return gap;
  }

  public void setGap(Integer gap) {
    this.gap = gap;
  }

  public Integer getInventoryValueCost() {
    return inventoryValueCost;
  }

  public void setInventoryValueCost(Integer inventoryValueCost) {
    this.inventoryValueCost = inventoryValueCost;
  }

  public Integer getQuantityInit() {
    return quantityInit;
  }

  public void setQuantityInit(Integer quantityInit) {
    this.quantityInit = quantityInit;
  }

  public Integer getQuantityOnHand() {
    return quantityOnHand;
  }

  public void setQuantityOnHand(Integer quantityOnHand) {
    this.quantityOnHand = quantityOnHand;
  }

  public String getProduitCip() {
    return produitCip;
  }

  public void setProduitCip(String produitCip) {
    this.produitCip = produitCip;
  }

  public String getProduitEan() {
    return produitEan;
  }

  public void setProduitEan(String produitEan) {
    this.produitEan = produitEan;
  }

  public String getProduitLibelle() {
    return produitLibelle;
  }

  public void setProduitLibelle(String produitLibelle) {
    this.produitLibelle = produitLibelle;
  }

  public String getRayonLibelle() {
    return rayonLibelle;
  }

  public void setRayonLibelle(String rayonLibelle) {
    this.rayonLibelle = rayonLibelle;
  }

  public String getStorageLibelle() {
    return storageLibelle;
  }

  public void setStorageLibelle(String storageLibelle) {
    this.storageLibelle = storageLibelle;
  }

  public Integer getPrixAchat() {
    return prixAchat;
  }

  public void setPrixAchat(Integer prixAchat) {
    this.prixAchat = prixAchat;
  }

  public Integer getPrixUni() {
    return prixUni;
  }

  public void setPrixUni(Integer prixUni) {
    this.prixUni = prixUni;
  }

  public Integer getLastUnitPrice() {
    return lastUnitPrice;
  }

  public void setLastUnitPrice(Integer lastUnitPrice) {
    this.lastUnitPrice = lastUnitPrice;
  }

  public BigInteger getRayonId() {
    return rayonId;
  }

  public void setRayonId(BigInteger rayonId) {
    this.rayonId = rayonId;
  }

  public BigInteger getStorageId() {
    return storageId;
  }

  public void setStorageId(BigInteger storageId) {
    this.storageId = storageId;
  }

  public String getRayonCode() {
    return rayonCode;
  }

  public void setRayonCode(String rayonCode) {
    this.rayonCode = rayonCode;
  }
}
