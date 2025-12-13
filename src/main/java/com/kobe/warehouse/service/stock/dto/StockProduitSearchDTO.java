package com.kobe.warehouse.service.stock.dto;

import com.kobe.warehouse.service.dto.StockProduitDTO;

import java.util.List;

public class StockProduitSearchDTO {
    private Integer id;
    private Integer produitId;
    private String produitLibelle;
    private String produitCodeCip;
    private Integer storageId;
    private String storageName;
    private String storageType;
    private Integer qtyStock;
    private Integer seuilMini;
    private List<StockProduitDTO> allStocks; // All stocks for this product

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getProduitCodeCip() {
        return produitCodeCip;
    }

    public void setProduitCodeCip(String produitCodeCip) {
        this.produitCodeCip = produitCodeCip;
    }

    public Integer getStorageId() {
        return storageId;
    }

    public void setStorageId(Integer storageId) {
        this.storageId = storageId;
    }

    public String getStorageName() {
        return storageName;
    }

    public void setStorageName(String storageName) {
        this.storageName = storageName;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public Integer getQtyStock() {
        return qtyStock;
    }

    public void setQtyStock(Integer qtyStock) {
        this.qtyStock = qtyStock;
    }

    public Integer getSeuilMini() {
        return seuilMini;
    }

    public void setSeuilMini(Integer seuilMini) {
        this.seuilMini = seuilMini;
    }

    public List<StockProduitDTO> getAllStocks() {
        return allStocks;
    }

    public void setAllStocks(List<StockProduitDTO> allStocks) {
        this.allStocks = allStocks;
    }
}
