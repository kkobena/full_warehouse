package com.kobe.warehouse.service.pharmaml.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class Annulation {

    @XmlAttribute(name = "Ref_Cde_Officine")
    private String refCdeOfficine;

    @XmlAttribute(name = "Ref_Cde_Repartiteur")
    private String refCdeRepartiteur;

    @XmlAttribute(name = "Motif")
    private String motif;

    public String getRefCdeOfficine() { return refCdeOfficine; }
    public Annulation setRefCdeOfficine(String refCdeOfficine) { this.refCdeOfficine = refCdeOfficine; return this; }

    public String getRefCdeRepartiteur() { return refCdeRepartiteur; }
    public Annulation setRefCdeRepartiteur(String refCdeRepartiteur) { this.refCdeRepartiteur = refCdeRepartiteur; return this; }

    public String getMotif() { return motif; }
    public Annulation setMotif(String motif) { this.motif = motif; return this; }
}
