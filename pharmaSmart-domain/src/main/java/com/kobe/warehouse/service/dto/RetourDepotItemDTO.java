package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class RetourDepotItemDTO {

    private Integer id;
    private Integer retourDepotId;
    private Long saleLineId;
    private String saleLineDate;
    private String produitLibelle;
    private String produitCip;
    private Integer produitId;

    @NotNull
    @Min(1)
    private Integer qtyMvt;

    private Integer regularUnitPrice;
    private Integer initStock;
    private Integer afterStock;
    private Integer saleLineQuantityRequested;
    private Integer saleLineQuantitySold;

    public Integer getId() {
        return id;
    }

    public RetourDepotItemDTO setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getRetourDepotId() {
        return retourDepotId;
    }

    public RetourDepotItemDTO setRetourDepotId(Integer retourDepotId) {
        this.retourDepotId = retourDepotId;
        return this;
    }

    public Long getSaleLineId() {
        return saleLineId;
    }

    public RetourDepotItemDTO setSaleLineId(Long saleLineId) {
        this.saleLineId = saleLineId;
        return this;
    }

    public String getSaleLineDate() {
        return saleLineDate;
    }

    public RetourDepotItemDTO setSaleLineDate(String saleLineDate) {
        this.saleLineDate = saleLineDate;
        return this;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public RetourDepotItemDTO setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
        return this;
    }

    public String getProduitCip() {
        return produitCip;
    }

    public RetourDepotItemDTO setProduitCip(String produitCip) {
        this.produitCip = produitCip;
        return this;
    }

    public Integer getProduitId() {
        return produitId;
    }

    public RetourDepotItemDTO setProduitId(Integer produitId) {
        this.produitId = produitId;
        return this;
    }

    public Integer getQtyMvt() {
        return qtyMvt;
    }

    public RetourDepotItemDTO setQtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
        return this;
    }

    public Integer getRegularUnitPrice() {
        return regularUnitPrice;
    }

    public RetourDepotItemDTO setRegularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
        return this;
    }

    public Integer getInitStock() {
        return initStock;
    }

    public RetourDepotItemDTO setInitStock(Integer initStock) {
        this.initStock = initStock;
        return this;
    }

    public Integer getAfterStock() {
        return afterStock;
    }

    public RetourDepotItemDTO setAfterStock(Integer afterStock) {
        this.afterStock = afterStock;
        return this;
    }

    public Integer getSaleLineQuantityRequested() {
        return saleLineQuantityRequested;
    }

    public RetourDepotItemDTO setSaleLineQuantityRequested(Integer saleLineQuantityRequested) {
        this.saleLineQuantityRequested = saleLineQuantityRequested;
        return this;
    }

    public Integer getSaleLineQuantitySold() {
        return saleLineQuantitySold;
    }

    public RetourDepotItemDTO setSaleLineQuantitySold(Integer saleLineQuantitySold) {
        this.saleLineQuantitySold = saleLineQuantitySold;
        return this;
    }
}
