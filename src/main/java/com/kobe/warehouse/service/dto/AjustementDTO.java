package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.MotifAjustement;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.service.dto.records.CodeLibelle;
import com.kobe.warehouse.service.pharmaml.dto.response.enumeration.CodeReponse;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class AjustementDTO {

    private Integer id;
    private int qtyMvt;
    @NotNull
    private Integer produitId;
    private Integer storageId;
    private Integer ajustId;
    private LocalDateTime dateMtv;
    private int stockBefore;
    private int stockAfter;
    private String produitLibelle;
    private String codeCip;
    private String userFullName;
    private Integer motifAjustementId;
    private String motifAjustementLibelle;
    private String commentaire;
    private CodeLibelle storageType;

    public AjustementDTO(Ajustement ajustement) {
        id = ajustement.getId();
        qtyMvt = ajustement.getQtyMvt();
        StockProduit stockProduit = ajustement.getStockProduit();
        Storage storage = stockProduit.getStorage();
        Produit produit = stockProduit.getProduit();
        storageType= new CodeLibelle(storage.getStorageType().name(), storage.getStorageType().getValue());
        produitId = produit.getId();
        ajustId = ajustement.getAjust().getId();
        dateMtv = ajustement.getDateMtv();
        stockBefore = ajustement.getStockBefore();
        stockAfter = ajustement.getStockAfter();
        produitLibelle = produit.getLibelle();
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();

        if (fournisseurProduit != null) {
            codeCip = fournisseurProduit.getCodeCip();
        }
        AppUser user = ajustement.getAjust().getUser();
        userFullName = user.getFirstName() + " " + user.getLastName();
        MotifAjustement motifAjustement = ajustement.getMotifAjustement();
        if (motifAjustement != null) {
            motifAjustementId = motifAjustement.getId();
            motifAjustementLibelle = motifAjustement.getLibelle();
        }
    }

    public CodeLibelle getStorageType() {
        return storageType;
    }

    public AjustementDTO setStorageType(CodeLibelle storageType) {
        this.storageType = storageType;
        return this;
    }

    public AjustementDTO() {}

    public void setProduitlibelle(String produitlibelle) {
        produitLibelle = produitlibelle;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getQtyMvt() {
        return qtyMvt;
    }

    public void setQtyMvt(int qtyMvt) {
        this.qtyMvt = qtyMvt;
    }

    public Integer getProduitId() {
        return produitId;
    }

    public void setProduitId(Integer produitId) {
        this.produitId = produitId;
    }

    public Integer getAjustId() {
        return ajustId;
    }

    public void setAjustId(Integer ajustId) {
        this.ajustId = ajustId;
    }

    public Integer getStorageId() {
        return storageId;
    }

    public AjustementDTO setStorageId(Integer storageId) {
        this.storageId = storageId;
        return this;
    }

    public LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public void setDateMtv(LocalDateTime dateMtv) {
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

    public AjustementDTO setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
        return this;
    }

    public String getCodeCip() {
        return codeCip;
    }

    public AjustementDTO setCodeCip(String codeCip) {
        this.codeCip = codeCip;
        return this;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public Integer getMotifAjustementId() {
        return motifAjustementId;
    }

    public AjustementDTO setMotifAjustementId(Integer motifAjustementId) {
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
}
