package com.kobe.warehouse.service.pharmaml.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class LigneInfoDemande {

    @XmlAttribute(name = "Code_Produit")
    private String codeProduit;

    @XmlAttribute(name = "Type_Codification")
    private String typeCodification;

    @XmlAttribute(name = "Quantite")
    private String quantite;

    public String getCodeProduit() { return codeProduit; }
    public LigneInfoDemande setCodeProduit(String codeProduit) { this.codeProduit = codeProduit; return this; }

    public String getTypeCodification() { return typeCodification; }
    public LigneInfoDemande setTypeCodification(String typeCodification) { this.typeCodification = typeCodification; return this; }

    public String getQuantite() { return quantite; }
    public LigneInfoDemande setQuantite(int quantite) {
        this.quantite = org.apache.commons.lang3.StringUtils.leftPad(quantite + "", 4, '0');
        return this;
    }
}
