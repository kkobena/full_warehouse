package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.PharmaMlStatut;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "pharmaml_envoi")
public class PharmaMlEnvoi implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumns({
        @JoinColumn(name = "commande_id",   referencedColumnName = "id"),
        @JoinColumn(name = "commande_date", referencedColumnName = "order_date"),
    })
    private Commande commande;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "fournisseur_id")
    private Fournisseur fournisseur;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private PharmaMlStatut statut = PharmaMlStatut.PENDING;

    @Column(name = "ref_message", length = 30)
    private String refMessage;

    @Column(name = "tentatives", nullable = false)
    private int tentatives = 0;

    @Column(name = "derniere_tentative")
    private LocalDateTime derniereTentative;

    @Column(name = "xml_requete_path")
    private String xmlRequetePath;

    @Column(name = "xml_reponse_path")
    private String xmlReponsePath;

    @Column(name = "total_lignes")
    private Integer totalLignes;

    @Column(name = "lignes_acceptees")
    private Integer lignesAcceptees;

    @Column(name = "lignes_rupture")
    private Integer lignesRupture;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Integer getId() { return id; }

    public Commande getCommande() { return commande; }
    public PharmaMlEnvoi setCommande(Commande commande) { this.commande = commande; return this; }

    public Fournisseur getFournisseur() { return fournisseur; }
    public PharmaMlEnvoi setFournisseur(Fournisseur fournisseur) { this.fournisseur = fournisseur; return this; }

    public PharmaMlStatut getStatut() { return statut; }
    public PharmaMlEnvoi setStatut(PharmaMlStatut statut) { this.statut = statut; return this; }

    public String getRefMessage() { return refMessage; }
    public PharmaMlEnvoi setRefMessage(String refMessage) { this.refMessage = refMessage; return this; }

    public int getTentatives() { return tentatives; }
    public PharmaMlEnvoi setTentatives(int tentatives) { this.tentatives = tentatives; return this; }

    public LocalDateTime getDerniereTentative() { return derniereTentative; }
    public PharmaMlEnvoi setDerniereTentative(LocalDateTime derniereTentative) { this.derniereTentative = derniereTentative; return this; }

    public String getXmlRequetePath() { return xmlRequetePath; }
    public PharmaMlEnvoi setXmlRequetePath(String xmlRequetePath) { this.xmlRequetePath = xmlRequetePath; return this; }

    public String getXmlReponsePath() { return xmlReponsePath; }
    public PharmaMlEnvoi setXmlReponsePath(String xmlReponsePath) { this.xmlReponsePath = xmlReponsePath; return this; }

    public Integer getTotalLignes() { return totalLignes; }
    public PharmaMlEnvoi setTotalLignes(Integer totalLignes) { this.totalLignes = totalLignes; return this; }

    public Integer getLignesAcceptees() { return lignesAcceptees; }
    public PharmaMlEnvoi setLignesAcceptees(Integer lignesAcceptees) { this.lignesAcceptees = lignesAcceptees; return this; }

    public Integer getLignesRupture() { return lignesRupture; }
    public PharmaMlEnvoi setLignesRupture(Integer lignesRupture) { this.lignesRupture = lignesRupture; return this; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
