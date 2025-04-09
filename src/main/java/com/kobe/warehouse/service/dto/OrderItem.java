package com.kobe.warehouse.service.dto;

public class OrderItem {

    private String produitCip;
    private String produitEan;
    private String produitLibelle;
    private int quantityRequested;
    private int quantityReceived;
    private String referenceBonLivraison;
    private String dateBonLivraison;
    private Double montant;
    private int ug;
    private double prixUn;
    private int prixAchat;
    private Double tva;
    private int ligne;
    private String facture;
    private String etablissement;
    private String datePeremption;
    private String lotNumber;

    public OrderItem() {}

    public String getEtablissement() {
        return etablissement;
    }

    public OrderItem setEtablissement(String etablissement) {
        this.etablissement = etablissement;
        return this;
    }

    public String getProduitCip() {
        return produitCip;
    }

    public OrderItem setProduitCip(String produitCip) {
        this.produitCip = produitCip;
        return this;
    }

    public String getProduitEan() {
        return produitEan;
    }

    public OrderItem setProduitEan(String produitEan) {
        this.produitEan = produitEan;
        return this;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public OrderItem setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
        return this;
    }

    public int getQuantityRequested() {
        return quantityRequested;
    }

    public OrderItem setQuantityRequested(int quantityRequested) {
        this.quantityRequested = quantityRequested;
        return this;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public OrderItem setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
        return this;
    }

    public int getQuantityReceived() {
        return quantityReceived;
    }

    public OrderItem setQuantityReceived(int quantityReceived) {
        this.quantityReceived = quantityReceived;
        return this;
    }

    public String getReferenceBonLivraison() {
        return referenceBonLivraison;
    }

    public OrderItem setReferenceBonLivraison(String referenceBonLivraison) {
        this.referenceBonLivraison = referenceBonLivraison;
        return this;
    }

    public String getDateBonLivraison() {
        return dateBonLivraison;
    }

    public OrderItem setDateBonLivraison(String dateBonLivraison) {
        this.dateBonLivraison = dateBonLivraison;
        return this;
    }

    public Double getMontant() {
        return montant;
    }

    public OrderItem setMontant(Double montant) {
        this.montant = montant;
        return this;
    }

    public int getUg() {
        return ug;
    }

    public OrderItem setUg(int ug) {
        this.ug = ug;
        return this;
    }

    public String getDatePeremption() {
        return datePeremption;
    }

    public OrderItem setDatePeremption(String datePeremption) {
        this.datePeremption = datePeremption;
        return this;
    }

    public double getPrixUn() {
        return prixUn;
    }

    public OrderItem setPrixUn(double prixUn) {
        this.prixUn = prixUn;
        return this;
    }

    public int getPrixAchat() {
        return prixAchat;
    }

    public OrderItem setPrixAchat(int prixAchat) {
        this.prixAchat = prixAchat;
        return this;
    }

    public Double getTva() {
        return tva;
    }

    public OrderItem setTva(Double tva) {
        this.tva = tva;
        return this;
    }

    public int getLigne() {
        return ligne;
    }

    public OrderItem setLigne(int ligne) {
        this.ligne = ligne;
        return this;
    }

    public String getFacture() {
        return facture;
    }

    public OrderItem setFacture(String facture) {
        this.facture = facture;
        return this;
    }
}
