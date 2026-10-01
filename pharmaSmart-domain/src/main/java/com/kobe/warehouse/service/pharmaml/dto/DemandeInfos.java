package com.kobe.warehouse.service.pharmaml.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class DemandeInfos {

    @NotNull
    @XmlAttribute(name = "Ref_Req_Info_Produit")
    private String refReqInfoProduit;
    @XmlElement(name = "LIGNE", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private List<LigneInfoDemande> lignes;

    public List<LigneInfoDemande> getLignes() {
        return lignes;
    }

    public DemandeInfos setLignes(List<LigneInfoDemande> lignes) {
        this.lignes = lignes;
        return this;
    }

    public String getRefReqInfoProduit() {
        return refReqInfoProduit;
    }

    public DemandeInfos setRefReqInfoProduit(String refReqInfoProduit) {
        this.refReqInfoProduit = refReqInfoProduit;
        return this;
    }
}
