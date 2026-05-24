package com.kobe.warehouse.service.pharmaml.dto.response;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "CSRP_ENVELOPPE", namespace = "urn:x-csrp:fr.csrp.protocole:enveloppe")
@XmlAccessorType(XmlAccessType.FIELD)
public class CsrpEnveloppeResponse {

    @XmlElement(name = "CORPS", namespace = "urn:x-csrp:fr.csrp.protocole:enveloppe")
    private CorpsResponse corps;

    // Getters et Setters
    public CorpsResponse getCorps() {
        return corps;
    }

    public void setCorps(CorpsResponse corps) {
        this.corps = corps;
    }
}
