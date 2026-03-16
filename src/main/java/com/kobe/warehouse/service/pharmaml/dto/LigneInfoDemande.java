package com.kobe.warehouse.service.pharmaml.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class LigneInfoDemande {

    @XmlAttribute(name = "Code_Produit")
    private String codeProduit;

    @XmlAttribute(name = "Type_Codification")
    private String typeCodification;

    @XmlAttribute(name = "Quantite")
    private int quantite;

    public String getCodeProduit() { return codeProduit; }
    public LigneInfoDemande setCodeProduit(String codeProduit) { this.codeProduit = codeProduit; return this; }

    public String getTypeCodification() { return typeCodification; }
    public LigneInfoDemande setTypeCodification(String typeCodification) { this.typeCodification = typeCodification; return this; }

    public int getQuantite() { return quantite; }
    public LigneInfoDemande setQuantite(int quantite) { this.quantite = quantite; return this; }
}
