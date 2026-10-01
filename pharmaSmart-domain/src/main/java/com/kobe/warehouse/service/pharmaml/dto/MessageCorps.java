package com.kobe.warehouse.service.pharmaml.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * @author koben
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class MessageCorps {

    // @XmlElement(name = "COMMANDE", namespace = "urn:x-csrp:fr.csrp.protocole:message-v02")
    @XmlElement(name = "COMMANDE", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private Commande commande;

    @XmlElement(name = "ACQ_RECEPTION", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private AcqReception acqReception;
    @XmlElement(name = "REQ_INFO_PRODUIT", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private DemandeInfos reqInfos;
    @XmlElement(name = "REQ_RETOUR", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private Retour retour;

    public Commande getCommande() {
        return commande;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }

    public DemandeInfos getReqInfos() {
        return reqInfos;
    }

    public void setReqInfos(DemandeInfos reqInfos) {
        this.reqInfos = reqInfos;
    }

    public AcqReception getAcqReception() {
        return acqReception;
    }

    public void setAcqReception(AcqReception acqReception) {
        this.acqReception = acqReception;
    }


    public Retour getRetour() {
        return retour;
    }

    public void setRetour(Retour retour) {
        this.retour = retour;
    }
}
