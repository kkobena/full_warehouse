package com.kobe.warehouse.service.pharmaml.dto;

import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class AcqReception {

    @XmlElement(name = "REF_CDE_OFFICINE", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private String refCdeOfficine;

    @XmlElement(name = "REF_CDE_REPARTITEUR", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private String refCdeRepartiteur;

    @XmlElement(name = "LIGNE_N", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private List<LigneReception> lignes;

    public String getRefCdeOfficine() { return refCdeOfficine; }
    public AcqReception setRefCdeOfficine(String refCdeOfficine) { this.refCdeOfficine = refCdeOfficine; return this; }

    public String getRefCdeRepartiteur() { return refCdeRepartiteur; }
    public AcqReception setRefCdeRepartiteur(String refCdeRepartiteur) { this.refCdeRepartiteur = refCdeRepartiteur; return this; }

    public List<LigneReception> getLignes() { return lignes; }
    public AcqReception setLignes(List<LigneReception> lignes) { this.lignes = lignes; return this; }
}
