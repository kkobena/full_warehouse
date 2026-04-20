package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.AvoirFournisseur;
import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import com.kobe.warehouse.domain.enumeration.AvoirStatut;

import java.time.LocalDateTime;

public class AvoirFournisseurDTO {

    private Integer id;
    private String reference;
    private LocalDateTime dateMtv;
    private long montant;
    private AvoirFournisseurStatut statut;
    private String commentaire;
    private Integer fournisseurId;
    private String fournisseurLibelle;
    private Integer reponseRetourBonId;
    private Integer retourBonId;
    private String retourBonReference;

    public AvoirFournisseurDTO() {}

    public AvoirFournisseurDTO(AvoirFournisseur avoir) {
        this.id = avoir.getId();
        this.reference = avoir.getReference();
        this.dateMtv = avoir.getDateMtv();
        this.montant = avoir.getMontant();
        this.statut = avoir.getStatut();
        this.commentaire = avoir.getCommentaire();
        this.fournisseurId = avoir.getFournisseur().getId();
        this.fournisseurLibelle = avoir.getFournisseur().getLibelle();
        this.reponseRetourBonId = avoir.getReponseRetourBon().getId();
        this.retourBonId = avoir.getReponseRetourBon().getRetourBon().getId();
        this.retourBonReference = avoir.getReponseRetourBon().getRetourBon().getReference();
    }

    public Integer getId() { return id; }
    public AvoirFournisseurDTO setId(Integer id) { this.id = id; return this; }

    public String getReference() { return reference; }
    public AvoirFournisseurDTO setReference(String reference) { this.reference = reference; return this; }

    public LocalDateTime getDateMtv() { return dateMtv; }
    public AvoirFournisseurDTO setDateMtv(LocalDateTime dateMtv) { this.dateMtv = dateMtv; return this; }

    public long getMontant() { return montant; }
    public AvoirFournisseurDTO setMontant(long montant) { this.montant = montant; return this; }

    public AvoirFournisseurStatut getStatut() { return statut; }
    public AvoirFournisseurDTO setStatut(AvoirFournisseurStatut statut) { this.statut = statut; return this; }

    public String getCommentaire() { return commentaire; }
    public AvoirFournisseurDTO setCommentaire(String commentaire) { this.commentaire = commentaire; return this; }

    public Integer getFournisseurId() { return fournisseurId; }
    public AvoirFournisseurDTO setFournisseurId(Integer fournisseurId) { this.fournisseurId = fournisseurId; return this; }

    public String getFournisseurLibelle() { return fournisseurLibelle; }
    public AvoirFournisseurDTO setFournisseurLibelle(String fournisseurLibelle) { this.fournisseurLibelle = fournisseurLibelle; return this; }

    public Integer getReponseRetourBonId() { return reponseRetourBonId; }
    public AvoirFournisseurDTO setReponseRetourBonId(Integer reponseRetourBonId) { this.reponseRetourBonId = reponseRetourBonId; return this; }

    public Integer getRetourBonId() { return retourBonId; }
    public AvoirFournisseurDTO setRetourBonId(Integer retourBonId) { this.retourBonId = retourBonId; return this; }

    public String getRetourBonReference() { return retourBonReference; }
    public AvoirFournisseurDTO setRetourBonReference(String retourBonReference) { this.retourBonReference = retourBonReference; return this; }
}
