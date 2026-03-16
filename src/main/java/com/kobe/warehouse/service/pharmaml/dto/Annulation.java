package com.kobe.warehouse.service.pharmaml.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Annulation {

    @XmlElement(name = "REF_CDE_OFFICINE", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private String refCdeOfficine;

    @XmlElement(name = "REF_CDE_REPARTITEUR", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private String refCdeRepartiteur;

    @XmlElement(name = "MOTIF", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private String motif;

    public String getRefCdeOfficine() { return refCdeOfficine; }
    public Annulation setRefCdeOfficine(String refCdeOfficine) { this.refCdeOfficine = refCdeOfficine; return this; }

    public String getRefCdeRepartiteur() { return refCdeRepartiteur; }
    public Annulation setRefCdeRepartiteur(String refCdeRepartiteur) { this.refCdeRepartiteur = refCdeRepartiteur; return this; }

    public String getMotif() { return motif; }
    public Annulation setMotif(String motif) { this.motif = motif; return this; }
}
