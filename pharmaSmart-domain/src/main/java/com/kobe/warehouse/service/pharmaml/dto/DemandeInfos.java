package com.kobe.warehouse.service.pharmaml.dto;

import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class DemandeInfos {

    @XmlElement(name = "LIGNE_INFO_DEMANDE", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private List<LigneInfoDemande> lignes;

    public List<LigneInfoDemande> getLignes() { return lignes; }
    public DemandeInfos setLignes(List<LigneInfoDemande> lignes) { this.lignes = lignes; return this; }
}
