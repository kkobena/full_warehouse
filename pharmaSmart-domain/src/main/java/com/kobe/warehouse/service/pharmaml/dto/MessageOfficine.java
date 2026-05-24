package com.kobe.warehouse.service.pharmaml.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * @author koben
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class MessageOfficine {

    @XmlElement(name = "ENTETE", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private MessageEntete entete;

    @XmlElement(name = "CORPS", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private MessageCorps corps;

    public MessageEntete getEntete() {
        return entete;
    }

    public void setEntete(MessageEntete entete) {
        this.entete = entete;
    }

    public MessageCorps getCorps() {
        return corps;
    }

    public void setCorps(MessageCorps corps) {
        this.corps = corps;
    }
}
