package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "avoir_fournisseur")
public class AvoirFournisseur implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "reference", length = 30, unique = true)
    private String reference;

    @NotNull
    @Column(name = "date_mtv", nullable = false)
    private LocalDateTime dateMtv = LocalDateTime.now();

    @NotNull
    @Column(name = "montant", nullable = false)
    private long montant;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private AvoirFournisseurStatut statut = AvoirFournisseurStatut.EN_ATTENTE;

    @Column(name = "commentaire", length = 200)
    private String commentaire;

    @NotNull
    @ManyToOne(optional = false)
    private AppUser user;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "retour_bon_id", nullable = false)
    private RetourBon retourBon;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @OneToMany(mappedBy = "avoirFournisseur", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AvoirFournisseurLine> lignes = new ArrayList<>();

    public Integer getId() { return id; }
    public AvoirFournisseur setId(Integer id) { this.id = id; return this; }

    public String getReference() { return reference; }
    public AvoirFournisseur setReference(String reference) { this.reference = reference; return this; }

    public LocalDateTime getDateMtv() { return dateMtv; }
    public AvoirFournisseur setDateMtv(LocalDateTime dateMtv) { this.dateMtv = dateMtv; return this; }

    public long getMontant() { return montant; }
    public AvoirFournisseur setMontant(long montant) { this.montant = montant; return this; }

    public AvoirFournisseurStatut getStatut() { return statut; }
    public AvoirFournisseur setStatut(AvoirFournisseurStatut statut) { this.statut = statut; return this; }

    public String getCommentaire() { return commentaire; }
    public AvoirFournisseur setCommentaire(String commentaire) { this.commentaire = commentaire; return this; }

    public AppUser getUser() { return user; }
    public AvoirFournisseur setUser(AppUser user) { this.user = user; return this; }

    public RetourBon getRetourBon() { return retourBon; }
    public AvoirFournisseur setRetourBon(RetourBon retourBon) { this.retourBon = retourBon; return this; }

    public Fournisseur getFournisseur() { return fournisseur; }
    public AvoirFournisseur setFournisseur(Fournisseur fournisseur) { this.fournisseur = fournisseur; return this; }

    public List<AvoirFournisseurLine> getLignes() { return lignes; }
    public AvoirFournisseur setLignes(List<AvoirFournisseurLine> lignes) { this.lignes = lignes; return this; }
}
