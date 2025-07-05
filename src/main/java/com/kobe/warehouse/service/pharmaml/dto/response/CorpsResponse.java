package com.kobe.warehouse.service.pharmaml.dto.response;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class CorpsResponse {

    @XmlElement(name = "MESSAGE_REPARTITEUR", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private MessageRepartiteur messageRepartiteur;

    // Getters et Setters
    public MessageRepartiteur getMessageRepartiteur() {
        return messageRepartiteur;
    }

    public void setMessageRepartiteur(MessageRepartiteur messageRepartiteur) {
        this.messageRepartiteur = messageRepartiteur;
    }
}
