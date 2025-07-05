package com.kobe.warehouse.service.pharmaml.dto.response;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class LigneNReponse {

    @XmlAttribute(name = "Code_Produit")
    private String codeProduit;

    @XmlAttribute(name = "Quantite_livree")
    private int quantiteLivree;

    @XmlElement(name = "PRIX_N", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private List<PrixN> prix;

    @XmlElement(name = "INDISPONIBILITE_N", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private IndisponibiliteN indisponibilite;

    // Getters et Setters
    public String getCodeProduit() {
        return codeProduit;
    }

    public void setCodeProduit(String codeProduit) {
        this.codeProduit = codeProduit;
    }

    public int getQuantiteLivree() {
        return quantiteLivree;
    }

    public void setQuantiteLivree(int quantiteLivree) {
        this.quantiteLivree = quantiteLivree;
    }

    public List<PrixN> getPrix() {
        return prix;
    }

    public void setPrix(List<PrixN> prix) {
        this.prix = prix;
    }

    public IndisponibiliteN getIndisponibilite() {
        return indisponibilite;
    }

    public void setIndisponibilite(IndisponibiliteN indisponibilite) {
        this.indisponibilite = indisponibilite;
    }
}
