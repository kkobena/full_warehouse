package com.kobe.warehouse.service.pharmaml.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * @author koben
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Commande {

    @XmlAttribute(name = "Ref_Cde_Client")
    private String refCdeClient;

    @XmlAttribute(name = "Commentaire_General")
    private String commentaireGeneral;

    @XmlAttribute(name = "Date_livraison")
    private String dateLivraison;

    @XmlElement(name = "NORMALE", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private Normale normale;

    @XmlElement(name = "EXCEPTIONNELLE", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private Exceptionnelle exceptionnelle;

    @XmlElement(name = "ACQ_RECEPTION", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private AcqReception acqReception;

    @XmlElement(name = "RETOUR", namespace = "urn:x-csrp:fr.csrp.protocole:message")
    private Retour retour;

    public String getRefCdeClient() { return refCdeClient; }
    public void setRefCdeClient(String refCdeClient) { this.refCdeClient = refCdeClient; }

    public String getCommentaireGeneral() { return commentaireGeneral; }
    public void setCommentaireGeneral(String commentaireGeneral) { this.commentaireGeneral = commentaireGeneral; }

    public String getDateLivraison() { return dateLivraison; }
    public void setDateLivraison(String dateLivraison) { this.dateLivraison = dateLivraison; }

    public Normale getNormale() { return normale; }
    public void setNormale(Normale normale) { this.normale = normale; }

    public Exceptionnelle getExceptionnelle() { return exceptionnelle; }
    public void setExceptionnelle(Exceptionnelle exceptionnelle) { this.exceptionnelle = exceptionnelle; }

    public AcqReception getAcqReception() { return acqReception; }
    public void setAcqReception(AcqReception acqReception) { this.acqReception = acqReception; }

    public Retour getRetour() { return retour; }
    public void setRetour(Retour retour) { this.retour = retour; }
}
