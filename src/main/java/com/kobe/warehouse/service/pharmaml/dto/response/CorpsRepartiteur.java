package com.kobe.warehouse.service.pharmaml.dto.response;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class CorpsRepartiteur {

    @XmlElement(name = "REP_COMMANDE", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private RepCommande repCommande;

    @XmlElement(name = "REP_INFOS", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private RepInfos repInfos;

    public RepCommande getRepCommande() {
        return repCommande;
    }

    public void setRepCommande(RepCommande repCommande) {
        this.repCommande = repCommande;
    }

    public RepInfos getRepInfos() {
        return repInfos;
    }

    public void setRepInfos(RepInfos repInfos) {
        this.repInfos = repInfos;
    }
}
