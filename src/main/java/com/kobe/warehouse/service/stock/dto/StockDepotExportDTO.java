package com.kobe.warehouse.service.stock.dto;

import com.alibaba.excel.annotation.ExcelProperty;

public class StockDepotExportDTO {

    @ExcelProperty("Produit ID")
    private Integer produitId;

    @ExcelProperty("Code CIP")
    private String code;

    @ExcelProperty("Produit")
    private String produitLibelle;

    @ExcelProperty("Code ean")
    private String codeEan;

    @ExcelProperty("Qté vendue")
    private Integer quantitySold;

    @ExcelProperty("Qté demandée")
    private Integer quantityRequested;

    @ExcelProperty("Prix unitaire")
    private Integer regularUnitPrice;

    @ExcelProperty("Valeur taxe")
    private Integer taxValue;

    @ExcelProperty("Prix achat")
    private Integer costAmount;

    public Integer getProduitId() {
        return produitId;
    }

    public void setProduitId(Integer produitId) {
        this.produitId = produitId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public void setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
    }

    public String getCodeEan() {
        return codeEan;
    }

    public void setCodeEan(String codeEan) {
        this.codeEan = codeEan;
    }

    public Integer getQuantitySold() {
        return quantitySold;
    }

    public void setQuantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
    }

    public Integer getQuantityRequested() {
        return quantityRequested;
    }

    public void setQuantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
    }

    public Integer getRegularUnitPrice() {
        return regularUnitPrice;
    }

    public void setRegularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
    }

    public Integer getTaxValue() {
        return taxValue;
    }

    public void setTaxValue(Integer taxValue) {
        this.taxValue = taxValue;
    }

    public Integer getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
    }

    public StockDepotExportDTO() {}

    public StockDepotExportDTO(
        Integer produitId,
        String code,
        String produitLibelle,
        String codeEan,
        Integer quantitySold,
        Integer quantityRequested,
        Integer regularUnitPrice,
        Integer taxValue,
        Integer costAmount
    ) {
        this.produitId = produitId;
        this.code = code;
        this.produitLibelle = produitLibelle;
        this.codeEan = codeEan;
        this.quantitySold = quantitySold;
        this.quantityRequested = quantityRequested;
        this.regularUnitPrice = regularUnitPrice;
        this.taxValue = taxValue;
        this.costAmount = costAmount;
    }
}
