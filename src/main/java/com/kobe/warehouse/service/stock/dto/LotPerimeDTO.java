package com.kobe.warehouse.service.stock.dto;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;

@ExcelIgnoreUnannotated
public class LotPerimeDTO {

    private Long produitId;

    private Long id;

    @ExcelProperty("Numéro de lot")
    private String numLot;

    @ExcelProperty("Fournisseur")
    private String fournisseur;

    @ExcelProperty("Nom du produit")
    private String produitName;

    @ExcelProperty("Code du produit")
    private String produitCode;

    @ExcelProperty("Date de péremption")
    private String datePeremption;

    @ExcelProperty("Quantité")
    private int quantity;

    @ExcelProperty("Prix d'achat")
    private int prixAchat;

    @ExcelProperty("Prix de vente")
    private int prixVente;

    @ExcelProperty("Prix total de vente")
    private int prixTotalVente;

    @ExcelProperty("Prix total d'achat")
    private int prixTotaAchat;

    @ExcelProperty("Statut de péremption")
    private String statutPerime;

    @ExcelProperty("Nom du rayon")
    private String rayonName;

    @ExcelProperty("Famille du produit")
    private String familleProduitName;

    private PeremptionStatut peremptionStatut;

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
}
