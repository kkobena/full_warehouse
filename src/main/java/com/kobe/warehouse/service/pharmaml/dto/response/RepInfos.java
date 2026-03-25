package com.kobe.warehouse.service.pharmaml.dto.response;

import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class RepInfos {

    @XmlElement(name = "LIGNE_INFO_REP", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private List<LigneInfoReponse> lignes;

    public List<LigneInfoReponse> getLignes() { return lignes; }
    public void setLignes(List<LigneInfoReponse> lignes) { this.lignes = lignes; }
}
