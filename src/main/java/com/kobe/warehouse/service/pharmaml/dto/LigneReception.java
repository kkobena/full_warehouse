package com.kobe.warehouse.service.pharmaml.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class LigneReception {

    @XmlAttribute(name = "Code_Produit")
    private String codeProduit;

    @XmlAttribute(name = "Type_Codification")
    private String typeCodification;

    @XmlAttribute(name = "Quantite_Recue")
    private int quantiteRecue;

    public String getCodeProduit() { return codeProduit; }
    public LigneReception setCodeProduit(String codeProduit) { this.codeProduit = codeProduit; return this; }

    public String getTypeCodification() { return typeCodification; }
    public LigneReception setTypeCodification(String typeCodification) { this.typeCodification = typeCodification; return this; }

    public int getQuantiteRecue() { return quantiteRecue; }
    public LigneReception setQuantiteRecue(int quantiteRecue) { this.quantiteRecue = quantiteRecue; return this; }
}
