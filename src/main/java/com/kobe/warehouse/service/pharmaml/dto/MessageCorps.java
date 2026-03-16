package com.kobe.warehouse.service.pharmaml.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * @author koben
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class MessageCorps {

    @XmlElement(name = "COMMANDE", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private Commande commande;

    @XmlElement(name = "DEMANDE_INFOS", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private DemandeInfos demandeInfos;

    @XmlElement(name = "ACQ_RECEPTION", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private AcqReception acqReception;

    @XmlElement(name = "ANNULATION", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private Annulation annulation;

    public Commande getCommande() { return commande; }
    public void setCommande(Commande commande) { this.commande = commande; }

    public DemandeInfos getDemandeInfos() { return demandeInfos; }
    public void setDemandeInfos(DemandeInfos demandeInfos) { this.demandeInfos = demandeInfos; }

    public AcqReception getAcqReception() { return acqReception; }
    public void setAcqReception(AcqReception acqReception) { this.acqReception = acqReception; }

    @XmlElement(name = "RETOUR", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private Retour retour;

    public Annulation getAnnulation() { return annulation; }
    public void setAnnulation(Annulation annulation) { this.annulation = annulation; }

    public Retour getRetour() { return retour; }
    public void setRetour(Retour retour) { this.retour = retour; }
}
