package com.kobe.warehouse.service.dto;


import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventoryLine;

import java.io.Serializable;

public class StoreInventoryLineDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private int quantityOnHand;
    private int quantityInit;
    private int quantitySold;
    private int inventoryValueCost;
    private int inventoryValueLatestSellingPrice;
    private Long storeInventoryId;
    private Long produitId;
    private String produitLibelle;
    private int inventoryValueTotalCost;
    private int inventoryValueAmount;
    private boolean updated;
   private Integer gap;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(int quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public int getQuantityInit() {
        return quantityInit;
    }

    public void setQuantityInit(int quantityInit) {
        this.quantityInit = quantityInit;
    }

    public int getQuantitySold() {
        return quantitySold;
    }

    public void setQuantitySold(int quantitySold) {
        this.quantitySold = quantitySold;
    }

    public int getInventoryValueCost() {
        return inventoryValueCost;
    }

    public void setInventoryValueCost(int inventoryValueCost) {
        this.inventoryValueCost = inventoryValueCost;
    }

    public int getInventoryValueLatestSellingPrice() {
        return inventoryValueLatestSellingPrice;
    }

    public void setInventoryValueLatestSellingPrice(int inventoryValueLatestSellingPrice) {
        this.inventoryValueLatestSellingPrice = inventoryValueLatestSellingPrice;
    }

    public Long getStoreInventoryId() {
        return storeInventoryId;
    }

    public void setStoreInventoryId(Long storeInventoryId) {
        this.storeInventoryId = storeInventoryId;
    }

    public Long getProduitId() {
        return produitId;
    }

    public void setProduitId(Long produitId) {
        this.produitId = produitId;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public void setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
    }

    public int getInventoryValueTotalCost() {
        return inventoryValueTotalCost;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public void setInventoryValueTotalCost(int inventoryValueTotalCost) {
        this.inventoryValueTotalCost = inventoryValueTotalCost;
    }

    public int getInventoryValueAmount() {
        return inventoryValueAmount;
    }

    public void setInventoryValueAmount(int inventoryValueAmount) {
        this.inventoryValueAmount = inventoryValueAmount;
    }

    public StoreInventoryLineDTO() {
    }

    public StoreInventoryLineDTO quantitySold(int quantitySold) {
        this.quantitySold = quantitySold;
        return this;
    }

    public StoreInventoryLineDTO(StoreInventoryLine storeInventoryLine) {
        this.id = storeInventoryLine.getId();
        this.gap=storeInventoryLine.getGap();
        this.quantityOnHand = storeInventoryLine.getQuantityOnHand();
        this.quantityInit = storeInventoryLine.getQuantityInit();
        Produit produit = storeInventoryLine.getProduit();
        this.inventoryValueCost = produit.getCostAmount();
        this.inventoryValueLatestSellingPrice = produit.getRegularUnitPrice();
        this.storeInventoryId = storeInventoryLine.getStoreInventory().getId();
        this.produitId = produit.getId();
        this.produitLibelle = produit.getLibelle();
        this.updated = storeInventoryLine.getUpdated();
        this.quantitySold = storeInventoryLine.getQuantitySold();
        this.inventoryValueTotalCost = produit.getCostAmount() * storeInventoryLine.getQuantityOnHand();
        this.inventoryValueAmount = storeInventoryLine.getQuantityOnHand() * storeInventoryLine.getQuantityOnHand();
    }

    public Integer getGap() {
        return gap;
    }

    public void setGap(Integer gap) {
        this.gap = gap;
    }
}
