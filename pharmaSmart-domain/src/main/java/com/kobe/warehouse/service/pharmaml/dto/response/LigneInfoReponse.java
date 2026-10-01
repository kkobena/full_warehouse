package com.kobe.warehouse.service.pharmaml.dto.response;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class LigneInfoReponse {// voir comment un objet parent pour regrouper les propriete communes
    @XmlAttribute(name = "Num_Ligne")
    private Integer numLigne;
    @XmlAttribute(name = "Num_Ligne_Demande")
    private Integer numLigneDemande;
    @XmlAttribute(name = "Code_Produit")
    private String codeProduit;
    @XmlAttribute(name = "Type_Codification")
    private String typeCodification;
    @XmlAttribute(name = "Designation")
    private String designation;

    @XmlElement(name = "PRIX", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private List<PrixN> prix;

    @XmlElement(name = "DISPO", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private Dispo dispo;

    @XmlElement(name = "NON_DISPO", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private NonDispo nonDispo;

    public Integer getNumLigne() {
        return numLigne;
    }

    public LigneInfoReponse setNumLigne(Integer numLigne) {
        this.numLigne = numLigne;
        return this;
    }

    public NonDispo getNonDispo() {
        return nonDispo;
    }

    public LigneInfoReponse setNonDispo(
        NonDispo nonDispo) {
        this.nonDispo = nonDispo;
        return this;
    }

    public Dispo getDispo() {
        return dispo;
    }

    public LigneInfoReponse setDispo(Dispo dispo) {
        this.dispo = dispo;
        return this;
    }

    public Integer getNumLigneDemande() {
        return numLigneDemande;
    }

    public LigneInfoReponse setNumLigneDemande(Integer numLigneDemande) {
        this.numLigneDemande = numLigneDemande;
        return this;
    }

    public String getTypeCodification() {
        return typeCodification;
    }

    public LigneInfoReponse setTypeCodification(String typeCodification) {
        this.typeCodification = typeCodification;
        return this;
    }

    public String getCodeProduit() {
        return codeProduit;
    }

    public void setCodeProduit(String codeProduit) {
        this.codeProduit = codeProduit;
    }


    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public List<PrixN> getPrix() {
        return prix;
    }

    public void setPrix(List<PrixN> prix) {
        this.prix = prix;
    }
}
