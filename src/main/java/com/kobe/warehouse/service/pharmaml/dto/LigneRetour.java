package com.kobe.warehouse.service.pharmaml.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class LigneRetour {

    @XmlAttribute(name = "Code_Produit")
    private String codeProduit;

    @XmlAttribute(name = "Type_Codification")
    private String typeCodification;

    @XmlAttribute(name = "Quantite")
    private int quantite;

    @XmlAttribute(name = "Motif_Retour")
    private String motifRetour;

    public String getCodeProduit() { return codeProduit; }
    public LigneRetour setCodeProduit(String codeProduit) { this.codeProduit = codeProduit; return this; }

    public String getTypeCodification() { return typeCodification; }
    public LigneRetour setTypeCodification(String typeCodification) { this.typeCodification = typeCodification; return this; }

    public int getQuantite() { return quantite; }
    public LigneRetour setQuantite(int quantite) { this.quantite = quantite; return this; }

    public String getMotifRetour() { return motifRetour; }
    public LigneRetour setMotifRetour(String motifRetour) { this.motifRetour = motifRetour; return this; }
}
