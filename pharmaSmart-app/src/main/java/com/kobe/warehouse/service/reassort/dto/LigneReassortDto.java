package com.kobe.warehouse.service.reassort.dto;

import com.kobe.warehouse.service.dto.StockProduitDTO;

public class LigneReassortDto {
    private Integer id;
    private Integer quantity;
    private Integer stockProduitId;
    private String produitLibelle;
    private String storageName;
    private Integer stockAvailable;
    private Integer seuilMini;
    private Integer stockActuel;
    private StockProduitDTO stockProduit;
    private String produitName;
    private String produitCode;
    private String codeEanFabricant;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public StockProduitDTO getStockProduit() {
        return stockProduit;
    }

    public void setStockProduit(StockProduitDTO stockProduit) {
        this.stockProduit = stockProduit;
    }

    public String getProduitName() {
        return produitName;
    }

    public void setProduitName(String produitName) {
        this.produitName = produitName;
    }

    public String getProduitCode() {
        return produitCode;
    }

    public void setProduitCode(String produitCode) {
        this.produitCode = produitCode;
    }

    public String getCodeEanFabricant() {
        return codeEanFabricant;
    }

    public void setCodeEanFabricant(String codeEanFabricant) {
        this.codeEanFabricant = codeEanFabricant;
    }

    public Integer getStockProduitId() {
        return stockProduitId;
    }

    public void setStockProduitId(Integer stockProduitId) {
        this.stockProduitId = stockProduitId;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public void setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
    }

    public String getStorageName() {
        return storageName;
    }

    public void setStorageName(String storageName) {
        this.storageName = storageName;
    }

    public Integer getStockAvailable() {
        return stockAvailable;
    }

    public void setStockAvailable(Integer stockAvailable) {
        this.stockAvailable = stockAvailable;
    }

    public Integer getSeuilMini() {
        return seuilMini;
    }

    public void setSeuilMini(Integer seuilMini) {
        this.seuilMini = seuilMini;
    }

    public Integer getStockActuel() {
        return stockActuel;
    }

    public void setStockActuel(Integer stockActuel) {
        this.stockActuel = stockActuel;
    }
}
