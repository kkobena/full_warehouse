package com.kobe.warehouse.service.product_to_destroy.dto;

import com.kobe.warehouse.service.stock.dto.PeremptionStatut;

public class ProductToDestroyDTO {

    private Long id;
    private String produitName;
    private String produitCodeCip;
    private String numLot;
    private int quantity;
    private String datePeremption;
    private String dateDestruction;
    private String user;
    private String createdDate;
    private String updatedDate;
    private String fournisseur;
    private int prixAchat;
    private int prixUni;
    private PeremptionStatut peremptionStatut;
    private boolean destroyed;
    private boolean editing;

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isEditing() {
        return editing;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
    }

    public String getDateDestruction() {
        return dateDestruction;
    }

    public void setDateDestruction(String dateDestruction) {
        this.dateDestruction = dateDestruction;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    public String getDatePeremption() {
        return datePeremption;
    }

    public void setDatePeremption(String datePeremption) {
        this.datePeremption = datePeremption;
    }

    public String getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(String fournisseur) {
        this.fournisseur = fournisseur;
    }

    public String getNumLot() {
        return numLot;
    }

    public void setNumLot(String numLot) {
        this.numLot = numLot;
    }

    public int getPrixAchat() {
        return prixAchat;
    }

    public void setPrixAchat(int prixAchat) {
        this.prixAchat = prixAchat;
    }

    public int getPrixUni() {
        return prixUni;
    }

    public void setPrixUni(int prixUni) {
        this.prixUni = prixUni;
    }

    public String getProduitCodeCip() {
        return produitCodeCip;
    }

    public void setProduitCodeCip(String produitCodeCip) {
        this.produitCodeCip = produitCodeCip;
    }

    public String getProduitName() {
        return produitName;
    }

    public void setProduitName(String produitName) {
        this.produitName = produitName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public PeremptionStatut getPeremptionStatut() {
        return peremptionStatut;
    }

    public void setPeremptionStatut(PeremptionStatut peremptionStatut) {
        this.peremptionStatut = peremptionStatut;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
