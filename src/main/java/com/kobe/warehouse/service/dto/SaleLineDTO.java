package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.service.sale.calculation.dto.Rate;
import java.time.LocalDateTime;
import java.util.List;

public class SaleLineDTO {

    private Long id;
    private Integer quantitySold;
    private Integer quantityRequested;
    private Integer regularUnitPrice;
    private Integer discountUnitPrice;
    private Integer netUnitPrice;
    private Integer discountAmount;
    private Integer salesAmount;
    private Integer htAmount;
    private Integer netAmount;
    private Integer taxAmount;
    private Integer costAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String produitLibelle, code;
    private Long produitId;
    private Long saleId;
    private Integer quantityStock;
    private Integer quantiyAvoir;
    private Integer calculationBasePrice;
    private Integer montantTvaUg = 0;
    private Integer quantityUg;
    private Integer amountToBeTakenIntoAccount;
    private boolean toIgnore;
    private LocalDateTime effectiveUpdateDate;
    private Integer taxValue;
    private boolean forceStock; // mis pour forcer le stock a la vente

    private List<Rate> rates;

    public SaleLineDTO() {}

    public SaleLineDTO(SalesLine salesLine) {
        super();
        id = salesLine.getId().getId();
        quantitySold = salesLine.getQuantitySold();
        quantityRequested = salesLine.getQuantityRequested();
        regularUnitPrice = salesLine.getRegularUnitPrice();
        discountUnitPrice = salesLine.getDiscountUnitPrice();
        netUnitPrice = salesLine.getNetUnitPrice();
        discountAmount = salesLine.getDiscountAmount();
        salesAmount = salesLine.getSalesAmount();
        costAmount = salesLine.getCostAmount();
        createdAt = salesLine.getCreatedAt();
        updatedAt = salesLine.getUpdatedAt();
        Produit produit = salesLine.getProduit();
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        if (fournisseurProduit != null) {
            code = fournisseurProduit.getCodeCip();
        }
        produitLibelle = produit.getLibelle();
        produitId = produit.getId();
        saleId = salesLine.getSales().getId().getId();
        quantiyAvoir = salesLine.getQuantityAvoir();
        calculationBasePrice = salesLine.getCalculationBasePrice();
        rates = salesLine
            .getRates();
    }



    public List<Rate> getRates() {
        return rates;
    }

    public void setRates(List<Rate> rates) {
        this.rates = rates;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public SaleLineDTO setQuantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
        return this;
    }

    public Integer getRegularUnitPrice() {
        return regularUnitPrice;
    }

    public void setRegularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
    }

    public Integer getDiscountUnitPrice() {
        return discountUnitPrice;
    }

    public void setDiscountUnitPrice(Integer discountUnitPrice) {
        this.discountUnitPrice = discountUnitPrice;
    }

    public Integer getNetUnitPrice() {
        return netUnitPrice;
    }

    public void setNetUnitPrice(Integer netUnitPrice) {
        this.netUnitPrice = netUnitPrice;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(Integer salesAmount) {
        this.salesAmount = salesAmount;
    }

    public Integer getHtAmount() {
        return htAmount;
    }

    public SaleLineDTO setHtAmount(Integer htAmount) {
        this.htAmount = htAmount;
        return this;
    }

    public Integer getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
    }

    public Integer getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
    }

    public Integer getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public void setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
    }

    public String getCode() {
        return code;
    }

    public SaleLineDTO setCode(String code) {
        this.code = code;
        return this;
    }

    public Integer getCalculationBasePrice() {
        return calculationBasePrice;
    }

    public void setCalculationBasePrice(Integer calculationBasePrice) {
        this.calculationBasePrice = calculationBasePrice;
    }

    public Long getProduitId() {
        return produitId;
    }

    public void setProduitId(Long produitId) {
        this.produitId = produitId;
    }

    public Long getSaleId() {
        return saleId;
    }

    public void setSaleId(Long saleId) {
        this.saleId = saleId;
    }

    public Integer getQuantityStock() {
        return quantityStock;
    }

    public void setQuantityStock(Integer quantityStock) {
        this.quantityStock = quantityStock;
    }

    public Integer getQuantiyAvoir() {
        return quantiyAvoir;
    }

    public SaleLineDTO setQuantiyAvoir(Integer quantiyAvoir) {
        this.quantiyAvoir = quantiyAvoir;
        return this;
    }

    public Integer getMontantTvaUg() {
        return montantTvaUg;
    }

    public SaleLineDTO setMontantTvaUg(Integer montantTvaUg) {
        this.montantTvaUg = montantTvaUg;
        return this;
    }

    public Integer getQuantityUg() {
        return quantityUg;
    }

    public SaleLineDTO setQuantityUg(Integer quantityUg) {
        this.quantityUg = quantityUg;
        return this;
    }

    public Integer getAmountToBeTakenIntoAccount() {
        return amountToBeTakenIntoAccount;
    }

    public SaleLineDTO setAmountToBeTakenIntoAccount(Integer amountToBeTakenIntoAccount) {
        this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
        return this;
    }

    public boolean isToIgnore() {
        return toIgnore;
    }

    public SaleLineDTO setToIgnore(boolean toIgnore) {
        this.toIgnore = toIgnore;
        return this;
    }

    public LocalDateTime getEffectiveUpdateDate() {
        return effectiveUpdateDate;
    }

    public SaleLineDTO setEffectiveUpdateDate(LocalDateTime effectiveUpdateDate) {
        this.effectiveUpdateDate = effectiveUpdateDate;
        return this;
    }

    public Integer getTaxValue() {
        return taxValue;
    }

    public SaleLineDTO setTaxValue(Integer taxValue) {
        this.taxValue = taxValue;
        return this;
    }

    public boolean isForceStock() {
        return forceStock;
    }

    public SaleLineDTO setForceStock(boolean forceStock) {
        this.forceStock = forceStock;
        return this;
    }
}
