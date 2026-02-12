package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Entité en lecture seule mappée sur la vue matérialisée mv_stock_valuation.
 * IMPORTANT: Cette entité est IMMUTABLE (lecture seule).
 */
@Entity
@Table(name = "mv_stock_valuation")
@Immutable // Hibernate: empêche INSERT/UPDATE/DELETE
public class MvStockValuationView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "produit_id")
    private Integer produitId;

    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Column(name = "code_cip")
    private String codeCip;

    @Column(name = "categorie")
    private String categorie;
    @Column(name = "categorie_id")
    private Integer categorieId;

    @Column(name = "purchase_price")
    private Integer purchasePrice;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "sales_price")
    private Integer salesPrice;

    @Column(name = "total_purchase_value")
    private Long totalPurchaseValue;

    @Column(name = "total_sales_value")
    private Long totalSalesValue;

    @Column(name = "potential_margin")
    private Long potentialMargin;

    @Column(name = "margin_percentage")
    private BigDecimal marginPercentage;

    public Integer getProduitId() {
        return produitId;
    }

    public void setProduitId(Integer produitId) {
        this.produitId = produitId;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getCodeCip() {
        return codeCip;
    }

    public void setCodeCip(String codeCip) {
        this.codeCip = codeCip;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public Integer getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(Integer purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getSalesPrice() {
        return salesPrice;
    }

    public void setSalesPrice(Integer salesPrice) {
        this.salesPrice = salesPrice;
    }

    public Long getTotalPurchaseValue() {
        return totalPurchaseValue;
    }

    public void setTotalPurchaseValue(Long totalPurchaseValue) {
        this.totalPurchaseValue = totalPurchaseValue;
    }

    public Long getTotalSalesValue() {
        return totalSalesValue;
    }

    public void setTotalSalesValue(Long totalSalesValue) {
        this.totalSalesValue = totalSalesValue;
    }

    public Long getPotentialMargin() {
        return potentialMargin;
    }

    public void setPotentialMargin(Long potentialMargin) {
        this.potentialMargin = potentialMargin;
    }

    public Integer getCategorieId() {
        return categorieId;
    }

    public void setCategorieId(Integer categorieId) {
        this.categorieId = categorieId;
    }

    public BigDecimal getMarginPercentage() {
        return marginPercentage;
    }

    public void setMarginPercentage(BigDecimal marginPercentage) {
        this.marginPercentage = marginPercentage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MvStockValuationView that)) return false;
        return produitId != null && produitId.equals(that.produitId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
