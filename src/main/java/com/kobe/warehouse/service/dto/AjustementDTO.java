package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.*;

import javax.validation.constraints.NotNull;
import java.time.Instant;

public class AjustementDTO {
    private Long id;
    private int qtyMvt;
    @NotNull
    private Long produitId;
    private Long ajustId;
    private Long storageId;
    private Instant dateMtv;
    private int stockBefore;
    private int stockAfter;
    private String produitLibelle,codeCip;
    private String userFullName;
    private Long motifAjustementId;
    private String motifAjustementLibelle;
    private String commentaire;

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
        Produit produit = ajustement.getProduit();
        this.produitId = produit.getId();
        this.ajustId = ajustement.getAjust().getId();
        this.dateMtv = ajustement.getDateMtv();
        this.stockBefore = ajustement.getStockBefore();
        this.stockAfter = ajustement.getStockAfter();
        this.produitLibelle = produit.getLibelle();
        FournisseurProduit fournisseurProduit=produit.getFournisseurProduitPrincipal();

        if(fournisseurProduit!=null){
            this.codeCip=fournisseurProduit.getCodeCip();
        }
        User user = ajustement.getAjust().getUser();
        this.userFullName = user.getFirstName() + " " + user.getLastName();
        MotifAjustement motifAjustement = ajustement.getMotifAjustement();
        if (motifAjustement != null) {
            this.motifAjustementId = motifAjustement.getId();
            this.motifAjustementLibelle = motifAjustement.getLibelle();
        }
    }

    public String getCodeCip() {
        return codeCip;
    }

    public AjustementDTO setCodeCip(String codeCip) {
        this.codeCip = codeCip;
        return this;
    }

    public AjustementDTO setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
        return this;
    }

    public Long getMotifAjustementId() {
        return motifAjustementId;
    }

    public AjustementDTO setMotifAjustementId(Long motifAjustementId) {
        this.motifAjustementId = motifAjustementId;
        return this;
    }

    public String getMotifAjustementLibelle() {
        return motifAjustementLibelle;
    }

    public AjustementDTO setMotifAjustementLibelle(String motifAjustementLibelle) {
        this.motifAjustementLibelle = motifAjustementLibelle;
        return this;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public AjustementDTO setCommentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }

    public Long getStorageId() {
        return storageId;
    }

    public AjustementDTO setStorageId(Long storageId) {
        this.storageId = storageId;
        return this;
    }

    public AjustementDTO() {
    }
}
