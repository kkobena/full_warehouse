package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.StockAlertType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "mv_stock_alerts")
@Immutable
public class MvStockAlert implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "produit_id")
    private Integer produitId;
    @Column(name = "stock_quantity")
    private Integer stockQuantity;
    @Column(name = "seuil_min")
    private Integer seuilMin;
    @Column(name = "libelle")
    private String libelle;
    @Column(name = "code_cip")
    private String codeCip;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type")
    private StockAlertType alertType;

    public Integer getProduitId() {
        return produitId;
    }

    public void setProduitId(Integer produitId) {
        this.produitId = produitId;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getSeuilMin() {
        return seuilMin;
    }

    public void setSeuilMin(Integer seuilMin) {
        this.seuilMin = seuilMin;
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

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public StockAlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(StockAlertType alertType) {
        this.alertType = alertType;
    }
}
