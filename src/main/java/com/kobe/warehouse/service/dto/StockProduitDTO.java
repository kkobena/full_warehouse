package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.StorageType;

import java.time.LocalDateTime;

public class StockProduitDTO {

    private Integer id;
    private int qtyStock;
    private int totalStockQuantity;
    private int qtyVirtual;
    private int qtyUG;
    private Integer storageId;
    private String storageName;
    private String storageType;
    private Integer produitId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String produitLibelle;
    private StorageType type;
    private Integer stockReassort;
    private Integer seuilMini;
    private Integer stockMaxi;
    private boolean withTransfer;
    public StockProduitDTO(StockProduit s) {
        this.id = s.getId();
        this.qtyStock = s.getQtyStock();
        this.qtyVirtual = s.getQtyVirtual();
        this.qtyUG = s.getQtyUG();
        Storage r = s.getStorage();
        this.storageId = r.getId();
        this.storageName = r.getName();
        this.storageType = r.getStorageType().getValue();
        this.type = r.getStorageType();
        Produit p = s.getProduit();
        this.produitId = p.getId();
        this.createdAt = p.getCreatedAt();
        this.updatedAt = p.getUpdatedAt();
        this.produitLibelle = p.getLibelle();
        this.stockReassort = s.getStockReassort();
        this.seuilMini = s.getSeuilMini();
        this.totalStockQuantity = s.getTotalStockQuantity();
        this. stockMaxi= s.getStockMaxi();
    }

    public int getTotalStockQuantity() {
        return totalStockQuantity;
    }

    public boolean isWithTransfer() {
        return withTransfer;
    }

    public void setWithTransfer(boolean withTransfer) {
        this.withTransfer = withTransfer;
    }

    public StockProduitDTO setTotalStockQuantity(int totalStockQuantity) {
        this.totalStockQuantity = totalStockQuantity;
        return this;
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

    public Integer getProduitId() {
        return produitId;
    }

    public StockProduitDTO setProduitId(Integer produitId) {
        this.produitId = produitId;
        return this;
    }

    public StorageType getType() {
        return type;
    }

    public StockProduitDTO setType(StorageType type) {
        this.type = type;
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

    public Integer getStorageId() {
        return storageId;
    }

    public StockProduitDTO setStorageId(Integer storageId) {
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

    public int getQtyStock() {
        return qtyStock;
    }

    public StockProduitDTO setQtyStock(int qtyStock) {
        this.qtyStock = qtyStock;
        return this;
    }

    public Integer getStockMaxi() {
        return stockMaxi;
    }

    public void setStockMaxi(Integer stockMaxi) {
        this.stockMaxi = stockMaxi;
    }

    public Integer getId() {
        return id;
    }

    public Integer getStockReassort() {
        return stockReassort;
    }

    public void setStockReassort(Integer stockReassort) {
        this.stockReassort = stockReassort;
    }

    public Integer getSeuilMini() {
        return seuilMini;
    }

    public void setSeuilMini(Integer seuilMini) {
        this.seuilMini = seuilMini;
    }

    public StockProduitDTO setId(Integer id) {
        this.id = id;
        return this;
    }
}
