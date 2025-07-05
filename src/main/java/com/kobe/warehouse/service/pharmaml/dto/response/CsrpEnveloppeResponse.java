package com.kobe.warehouse.service.pharmaml.dto.response;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
