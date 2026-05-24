package com.kobe.warehouse.service.pharmaml.dto.response;

import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class LigneInfoReponse {

    @XmlAttribute(name = "Code_Produit")
    private String codeProduit;

    @XmlAttribute(name = "Stock_Disponible")
    private int stockDisponible;

    @XmlAttribute(name = "Designation")
    private String designation;

    @XmlElement(name = "PRIX_N", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private List<PrixN> prix;

    public String getCodeProduit() { return codeProduit; }
    public void setCodeProduit(String codeProduit) { this.codeProduit = codeProduit; }

    public int getStockDisponible() { return stockDisponible; }
    public void setStockDisponible(int stockDisponible) { this.stockDisponible = stockDisponible; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public List<PrixN> getPrix() { return prix; }
    public void setPrix(List<PrixN> prix) { this.prix = prix; }
}
