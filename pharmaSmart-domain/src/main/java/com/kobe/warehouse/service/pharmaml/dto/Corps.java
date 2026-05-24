package com.kobe.warehouse.service.pharmaml.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * @author koben
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Corps {

    @XmlElement(name = "MESSAGE_OFFICINE", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private MessageOfficine messageOfficine;

    public MessageOfficine getMessageOfficine() {
        return messageOfficine;
    }

    public void setMessageOfficine(MessageOfficine messageOfficine) {
        this.messageOfficine = messageOfficine;
    }
}
