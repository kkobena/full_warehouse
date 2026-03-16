package com.kobe.warehouse.service.pharmaml.dto;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Retour {

    @XmlElement(name = "REF_CDE_OFFICINE", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private String refCdeOfficine;

    @XmlElement(name = "REF_BL", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private String refBl;

    @XmlElement(name = "LIGNE_RETOUR", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private List<LigneRetour> lignes;

    public String getRefCdeOfficine() { return refCdeOfficine; }
    public Retour setRefCdeOfficine(String refCdeOfficine) { this.refCdeOfficine = refCdeOfficine; return this; }

    public String getRefBl() { return refBl; }
    public Retour setRefBl(String refBl) { this.refBl = refBl; return this; }

    public List<LigneRetour> getLignes() { return lignes; }
    public Retour setLignes(List<LigneRetour> lignes) { this.lignes = lignes; return this; }
}
