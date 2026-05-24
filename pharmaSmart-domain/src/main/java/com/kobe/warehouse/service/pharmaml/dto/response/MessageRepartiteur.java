package com.kobe.warehouse.service.pharmaml.dto.response;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class MessageRepartiteur {

    @XmlElement(name = "CORPS", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private CorpsRepartiteur corps;

    // Getters et Setters
    public CorpsRepartiteur getCorps() {
        return corps;
    }

    public void setCorps(CorpsRepartiteur corps) {
        this.corps = corps;
    }
}
