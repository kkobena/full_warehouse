package com.kobe.warehouse.service.pharmaml.dto;

import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * @author koben
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Normale {

    @XmlElement(name = "LIGNE_N", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private List<LigneN> lignes;

    public List<LigneN> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneN> lignes) {
        this.lignes = lignes;
    }
}
