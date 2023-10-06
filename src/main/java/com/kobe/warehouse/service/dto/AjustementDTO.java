package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.MotifAjustement;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.User;
import java.time.LocalDateTime;
import javax.validation.constraints.NotNull;

public class AjustementDTO {
  private Long id;
  private int qtyMvt;
  @NotNull private Long produitId;
  private Long ajustId;
  private Long storageId;
  private LocalDateTime dateMtv;
  private int stockBefore;
  private int stockAfter;
  private String produitLibelle;
  private String codeCip;
  private String userFullName;
  private Long motifAjustementId;
  private String motifAjustementLibelle;
  private String commentaire;

  public AjustementDTO(Ajustement ajustement) {
    id = ajustement.getId();
    qtyMvt = ajustement.getQtyMvt();
    Produit produit = ajustement.getProduit();
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
    User user = ajustement.getAjust().getUser();
    userFullName = user.getFirstName() + " " + user.getLastName();
    MotifAjustement motifAjustement = ajustement.getMotifAjustement();
    if (motifAjustement != null) {
      motifAjustementId = motifAjustement.getId();
      motifAjustementLibelle = motifAjustement.getLibelle();
    }
  }

  public AjustementDTO() {}

  public void setProduitlibelle(String produitlibelle) {
    produitLibelle = produitlibelle;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public int getQtyMvt() {
    return qtyMvt;
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

  public Long getAjustId() {
    return ajustId;
  }

  public void setAjustId(Long ajustId) {
    this.ajustId = ajustId;
  }

  public Long getStorageId() {
    return storageId;
  }

  public AjustementDTO setStorageId(Long storageId) {
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
}
