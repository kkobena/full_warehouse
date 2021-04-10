package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.User;

import javax.validation.constraints.NotNull;
import java.time.Instant;

public class AjustementDTO {
    private  Long id;
    private int qtyMvt;
    @NotNull
    private Long produitId;
    private Long ajustId;
    private Instant dateMtv;
    private int stockBefore;
    private int stockAfter;
    private String produitLibelle;
    private String userFullName;
    public int getQtyMvt() {
        return qtyMvt;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public void setQtyMvt(int qtyMvt) {
        this.qtyMvt = qtyMvt;
    }

    public Long getProduitId() {
        return produitId;
    }

    public void setProduitId(Long produitId) {
        this.produitId = produitId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAjustId() {
        return ajustId;
    }

    public void setAjustId(Long ajustId) {
        this.ajustId = ajustId;
    }

    public Instant getDateMtv() {
        return dateMtv;
    }

    public void setDateMtv(Instant dateMtv) {
        this.dateMtv = dateMtv;
    }

    public int getStockBefore() {
        return stockBefore;
    }

    public void setStockBefore(int stockBefore) {
        this.stockBefore = stockBefore;
    }

    public int getStockAfter() {
        return stockAfter;
    }

    public void setStockAfter(int stockAfter) {
        this.stockAfter = stockAfter;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public void setProduitlibelle(String produitlibelle) {
        this.produitLibelle = produitlibelle;
    }

    public AjustementDTO(Ajustement ajustement) {
        this.id = ajustement.getId();
        this.qtyMvt = ajustement.getQtyMvt();
        Produit produit=ajustement.getProduit();
        this.produitId = produit.getId();
        this.ajustId = ajustement.getAjust().getId();
        this.dateMtv = ajustement.getDateMtv();
        this.stockBefore = ajustement.getStockBefore();
        this.stockAfter = ajustement.getStockAfter();
        this.produitLibelle = produit.getLibelle();
        User user=ajustement.getAjust().getUser();
        this.userFullName=user.getFirstName()+" "+user.getLastName();
    }

    public AjustementDTO() {
    }
}
