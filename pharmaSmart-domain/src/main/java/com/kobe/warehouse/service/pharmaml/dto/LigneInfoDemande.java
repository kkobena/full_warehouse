package com.kobe.warehouse.service.pharmaml.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;

@XmlAccessorType(XmlAccessType.FIELD)
public class LigneInfoDemande {

    @NotNull
    @XmlAttribute(name = "Code_Produit")
    private String codeProduit;
    @NotNull
    @XmlAttribute(name = "Type_Codification")
    private String typeCodification;
    @XmlAttribute(name = "Designation")
    private String designation;

    @XmlAttribute(name = "Quantite")
    private int quantite;

    @XmlAttribute(name = "Num_Ligne")
    private int numLigne;
    @XmlTransient
    private int prixAchat;//prix d'achat du produit en machine
    @XmlTransient
    private int prixVente;//prix de vente du produit en machine


    public String getDesignation() {
        return designation;
    }

    public LigneInfoDemande setDesignation(String designation) {
        this.designation = designation;
        return this;
    }

    public String getCodeProduit() {
        return codeProduit;
    }

    public LigneInfoDemande setCodeProduit(String codeProduit) {
        this.codeProduit = codeProduit;
        return this;
    }

    public String getTypeCodification() {
        return typeCodification;
    }

    public LigneInfoDemande setTypeCodification(String typeCodification) {
        this.typeCodification = typeCodification;
        return this;
    }

    public int getQuantite() {
        return quantite;
    }

    public LigneInfoDemande setQuantite(int quantite) {
        //  this.quantite = org.apache.commons.lang3.StringUtils.leftPad(quantite + "", 4, '0');
        this.quantite = quantite;
        return this;
    }

    public int getNumLigne() {
        return numLigne;
    }

    public int getPrixAchat() {
        return prixAchat;
    }

    public LigneInfoDemande setPrixAchat(int prixAchat) {
        this.prixAchat = prixAchat;
        return this;
    }

    public int getPrixVente() {
        return prixVente;
    }

    public LigneInfoDemande setPrixVente(int prixVente) {
        this.prixVente = prixVente;
        return this;
    }

    public LigneInfoDemande setNumLigne(int numLigne) {
        this.numLigne = numLigne;
        return this;
    }
}
