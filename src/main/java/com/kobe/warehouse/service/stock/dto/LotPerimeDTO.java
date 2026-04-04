package com.kobe.warehouse.service.stock.dto;

import java.util.List;

public class LotPerimeDTO {

    // ...existing fields...
    private Integer produitId;
    private Integer id;
    private String numLot;
    private String fournisseur;
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
    private PeremptionStatut peremptionStatut;

    /**
     * Localisations où ce lot est présent avec du stock.
     * Vide = lot sans LotStockLocation connu (mode sans gestion multi-emplacement).
     * Taille 1 = lot dans un seul emplacement.
     * Taille > 1 = lot multi-emplacement → l'utilisateur doit choisir.
     */
    private List<LotLocationDTO> locations = List.of();

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

    public String getFournisseur() {
        return fournisseur;
    }

    public LotPerimeDTO setFournisseur(String fournisseur) {
        this.fournisseur = fournisseur;
        return this;
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

    public PeremptionStatut getPeremptionStatut() {
        return peremptionStatut;
    }

    public void setPeremptionStatut(PeremptionStatut peremptionStatut) {
        this.peremptionStatut = peremptionStatut;
    }

    public Integer getProduitId() {
        return produitId;
    }

    public void setProduitId(Integer produitId) {
        this.produitId = produitId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<LotLocationDTO> getLocations() {
        return locations;
    }

    public void setLocations(List<LotLocationDTO> locations) {
        this.locations = locations != null ? locations : List.of();
    }
}
