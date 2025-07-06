package com.kobe.warehouse.service.stock.dto;

public class LotPerimeDTO {

    private String numLot;

    private String founisseur;
    private String produitName;
    private String produitCode;
    private String datePeremption;
    private int quantity;
    private int prixAchat;
    private int prixVente;
    private int prixTotalVente;
    private int prixTotaAchat;
    private String statutPerime;
    private String rayonName;
    private String familleProduitName;

    public String getDatePeremption() {
        return datePeremption;
    }

    public void setDatePeremption(String datePeremption) {
        this.datePeremption = datePeremption;
    }

    public String getFamilleProduitName() {
        return familleProduitName;
    }

    public void setFamilleProduitName(String familleProduitName) {
        this.familleProduitName = familleProduitName;
    }

    public String getFounisseur() {
        return founisseur;
    }

    public void setFounisseur(String founisseur) {
        this.founisseur = founisseur;
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

    public int getPrixTotaAchat() {
        return prixTotaAchat;
    }

    public void setPrixTotaAchat(int prixTotaAchat) {
        this.prixTotaAchat = prixTotaAchat;
    }

    public int getPrixTotalVente() {
        return prixTotalVente;
    }

    public void setPrixTotalVente(int prixTotalVente) {
        this.prixTotalVente = prixTotalVente;
    }

    public int getPrixVente() {
        return prixVente;
    }

    public void setPrixVente(int prixVente) {
        this.prixVente = prixVente;
    }

    public String getProduitCode() {
        return produitCode;
    }

    public void setProduitCode(String produitCode) {
        this.produitCode = produitCode;
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

    public String getRayonName() {
        return rayonName;
    }

    public void setRayonName(String rayonName) {
        this.rayonName = rayonName;
    }

    public String getStatutPerime() {
        return statutPerime;
    }

    public void setStatutPerime(String statutPerime) {
        this.statutPerime = statutPerime;
    }
}
