package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
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
    private Integer montantTvaUg = 0;
    private Integer quantityUg;
    private Integer amountToBeTakenIntoAccount;
    private boolean toIgnore;
    private LocalDateTime effectiveUpdateDate;
    private Integer taxValue;
    private boolean forceStock; // mis pour forcer le stock a la vente

    public SaleLineDTO() {
    }

    public SaleLineDTO(SalesLine salesLine) {
        super();
        id = salesLine.getId();
        quantitySold = salesLine.getQuantitySold();
        quantityRequested = salesLine.getQuantityRequested();
        regularUnitPrice = salesLine.getRegularUnitPrice();
        discountUnitPrice = salesLine.getDiscountUnitPrice();
        netUnitPrice = salesLine.getNetUnitPrice();
        discountAmount = salesLine.getDiscountAmount();
        salesAmount = salesLine.getSalesAmount();
        netAmount = salesLine.getNetAmount();
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
        saleId = salesLine.getSales().getId();
        quantiyAvoir = salesLine.getQuantityAvoir();
    }

  public SaleLineDTO setForceStock(boolean forceStock) {
        this.forceStock = forceStock;
        return this;
    }

  public SaleLineDTO setHtAmount(Integer htAmount) {
        this.htAmount = htAmount;
        return this;
    }

  public SaleLineDTO setTaxValue(Integer taxValue) {
        this.taxValue = taxValue;
        return this;
    }

  public SaleLineDTO setAmountToBeTakenIntoAccount(Integer amountToBeTakenIntoAccount) {
        this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
        return this;
    }

  public SaleLineDTO setToIgnore(boolean toIgnore) {
        this.toIgnore = toIgnore;
        return this;
    }

  public SaleLineDTO setEffectiveUpdateDate(LocalDateTime effectiveUpdateDate) {
        this.effectiveUpdateDate = effectiveUpdateDate;
        return this;
    }

  public SaleLineDTO setQuantityUg(Integer quantityUg) {
        this.quantityUg = quantityUg;
        return this;
    }

  public SaleLineDTO setQuantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
        return this;
    }

  public SaleLineDTO setQuantiyAvoir(Integer quantiyAvoir) {
        this.quantiyAvoir = quantiyAvoir;
        return this;
    }

  public SaleLineDTO setMontantTvaUg(Integer montantTvaUg) {
        this.montantTvaUg = montantTvaUg;
        return this;
    }

  public void setId(Long id) {
        this.id = id;
    }

  public void setQuantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
    }

  public void setRegularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
    }

  public void setDiscountUnitPrice(Integer discountUnitPrice) {
        this.discountUnitPrice = discountUnitPrice;
    }

  public void setNetUnitPrice(Integer netUnitPrice) {
        this.netUnitPrice = netUnitPrice;
    }

  public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }

  public void setSalesAmount(Integer salesAmount) {
        this.salesAmount = salesAmount;
    }

  public void setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
    }

  public void setTaxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
    }

  public void setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
    }

  public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

  public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

  public void setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
    }

  public void setProduitId(Long produitId) {
        this.produitId = produitId;
    }

  public SaleLineDTO setCode(String code) {
        this.code = code;
        return this;
    }

  public void setQuantityStock(Integer quantityStock) {
        this.quantityStock = quantityStock;
    }

  public void setSaleId(Long saleId) {
        this.saleId = saleId;
    }
}
