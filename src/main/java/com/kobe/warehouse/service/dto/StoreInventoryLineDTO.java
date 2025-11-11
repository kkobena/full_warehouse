package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventoryLine;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

public class StoreInventoryLineDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private Integer quantityOnHand;
    private Integer quantityInit;
    private Integer quantitySold;
    private Integer inventoryValueCost;
    private Integer inventoryValueLatestSellingPrice;
    private Long storeInventoryId;
    private Integer produitId;
    private String produitLibelle;
    private int inventoryValueTotalCost;
    private int inventoryValueAmount;
    private boolean updated;
    private Integer gap;
    private Integer prixAchat;
    private Integer prixUni;
    private String produitCip;
    private String produitEan;
    private long rayonId;
    private Set<String> produitCips;

    public StoreInventoryLineDTO() {}

    public StoreInventoryLineDTO(StoreInventoryLine storeInventoryLine) {
        this.id = storeInventoryLine.getId();
        this.gap = storeInventoryLine.getGap();
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
        if (Objects.nonNull(storeInventoryLine.getQuantityOnHand())) {
            this.inventoryValueTotalCost = produit.getCostAmount() * storeInventoryLine.getQuantityOnHand();
            this.inventoryValueAmount = storeInventoryLine.getQuantityOnHand() * storeInventoryLine.getLastUnitPrice();
        }

    }

    public long getRayonId() {
        return rayonId;
    }

    public StoreInventoryLineDTO setRayonId(long rayonId) {
        this.rayonId = rayonId;
        return this;
    }

    public String getProduitCip() {
        return produitCip;
    }

    public StoreInventoryLineDTO setProduitCip(String produitCip) {
        this.produitCip = produitCip;
        return this;
    }

    public String getProduitEan() {
        return produitEan;
    }

    public StoreInventoryLineDTO setProduitEan(String produitEan) {
        this.produitEan = produitEan;
        return this;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuantityOnHand() {
        return quantityOnHand;
    }

    public StoreInventoryLineDTO setQuantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
        return this;
    }

    public Integer getQuantityInit() {
        return quantityInit;
    }

    public StoreInventoryLineDTO setQuantityInit(Integer quantityInit) {
        this.quantityInit = quantityInit;
        return this;
    }

    public Integer getQuantitySold() {
        return quantitySold;
    }

    public StoreInventoryLineDTO setQuantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
        return this;
    }

    public Set<String> getProduitCips() {
        return produitCips;
    }

    public StoreInventoryLineDTO setProduitCips(Set<String> produitCips) {
        this.produitCips = produitCips;
        return this;
    }

    public Integer getInventoryValueCost() {
        return inventoryValueCost;
    }

    public StoreInventoryLineDTO setInventoryValueCost(Integer inventoryValueCost) {
        this.inventoryValueCost = inventoryValueCost;
        return this;
    }

    public Integer getInventoryValueLatestSellingPrice() {
        return inventoryValueLatestSellingPrice;
    }

    public StoreInventoryLineDTO setInventoryValueLatestSellingPrice(Integer inventoryValueLatestSellingPrice) {
        this.inventoryValueLatestSellingPrice = inventoryValueLatestSellingPrice;
        return this;
    }

    public int getInventoryValueTotalCost() {
        return inventoryValueTotalCost;
    }

    public StoreInventoryLineDTO setInventoryValueTotalCost(int inventoryValueTotalCost) {
        this.inventoryValueTotalCost = inventoryValueTotalCost;
        return this;
    }

    public int getInventoryValueAmount() {
        return inventoryValueAmount;
    }

    public StoreInventoryLineDTO setInventoryValueAmount(int inventoryValueAmount) {
        this.inventoryValueAmount = inventoryValueAmount;
        return this;
    }

    public Integer getPrixAchat() {
        return prixAchat;
    }

    public StoreInventoryLineDTO setPrixAchat(Integer prixAchat) {
        this.prixAchat = prixAchat;
        return this;
    }

    public Integer getPrixUni() {
        return prixUni;
    }

    public StoreInventoryLineDTO setPrixUni(Integer prixUni) {
        this.prixUni = prixUni;
        return this;
    }

    public Long getStoreInventoryId() {
        return storeInventoryId;
    }

    public void setStoreInventoryId(Long storeInventoryId) {
        this.storeInventoryId = storeInventoryId;
    }

    public Integer getProduitId() {
        return produitId;
    }

    public void setProduitId(Integer produitId) {
        this.produitId = produitId;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public void setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public Integer getGap() {
        return gap;
    }

    public void setGap(Integer gap) {
        this.gap = gap;
    }
}
