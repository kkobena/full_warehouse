package com.kobe.warehouse.service.reassort.dto;

import com.kobe.warehouse.service.dto.StockProduitDTO;

import java.time.LocalDateTime;

public class RepartitionStockProduitDto {
    private Integer id;
    private String userFullName;
    private Integer mvtQty;
    private LocalDateTime created;
    private String produitName;
    private String produitCode;
    private String codeEanFabricant;
    private StockProduitDTO stockProduitSrc;
    private StockProduitDTO stockProduitDest;
    private Integer destFinalStock;
    private Integer destInitStock;
    private Integer sourceFinalStock;
    private Integer sourceInitStock;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public Integer getMvtQty() {
        return mvtQty;
    }

    public void setMvtQty(Integer mvtQty) {
        this.mvtQty = mvtQty;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
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

    public StockProduitDTO getStockProduitSrc() {
        return stockProduitSrc;
    }

    public void setStockProduitSrc(StockProduitDTO stockProduitSrc) {
        this.stockProduitSrc = stockProduitSrc;
    }

    public StockProduitDTO getStockProduitDest() {
        return stockProduitDest;
    }

    public void setStockProduitDest(StockProduitDTO stockProduitDest) {
        this.stockProduitDest = stockProduitDest;
    }

    public Integer getDestFinalStock() {
        return destFinalStock;
    }

    public void setDestFinalStock(Integer destFinalStock) {
        this.destFinalStock = destFinalStock;
    }

    public Integer getDestInitStock() {
        return destInitStock;
    }

    public void setDestInitStock(Integer destInitStock) {
        this.destInitStock = destInitStock;
    }

    public Integer getSourceFinalStock() {
        return sourceFinalStock;
    }

    public void setSourceFinalStock(Integer sourceFinalStock) {
        this.sourceFinalStock = sourceFinalStock;
    }

    public Integer getSourceInitStock() {
        return sourceInitStock;
    }

    public void setSourceInitStock(Integer sourceInitStock) {
        this.sourceInitStock = sourceInitStock;
    }
}
