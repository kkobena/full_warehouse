package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.RetourStatut;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumns;
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
@Table(name = "retour_bon")
public class RetourBon implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "date_mtv", nullable = false)
    private LocalDateTime dateMtv = LocalDateTime.now();

    @ManyToOne(optional = false)
    @NotNull
    private AppUser user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 15)
    private RetourStatut statut = RetourStatut.VALIDATED;

    @Column(name = "commentaire", length = 150)
    private String commentaire;

    @OneToMany(mappedBy = "retourBon")
    private List<RetourBonItem> retourBonItems = new ArrayList<>();

    @ManyToOne
    @JoinColumns(
        {
            @JoinColumn(name = "commande_id", referencedColumnName = "id"),
            @JoinColumn(name = "commande_order_date", referencedColumnName = "order_date"),
        }
    )
    private Commande commande;

    /** Fournisseur direct — renseigné quand horsCommande = true (déduit via FournisseurProduit.principal) */
    @ManyToOne
    @JoinColumn(name = "fournisseur_id")
    private Fournisseur fournisseur;

    /** true = bon de retour créé sans référence à une commande (lot hors entrée en stock) */
    @Column(name = "hors_commande", nullable = false)
    private boolean horsCommande = false;

    @OneToMany(mappedBy = "retourBon")
    private List<ReponseRetourBon> reponseRetourBons = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "pharmaml_envoi_id")
    private PharmaMlEnvoi pharmamlEnvoi;

    public Integer getId() {
        return id;
    }

    public RetourBon setId(Integer id) {
        this.id = id;
        return this;
    }

    public Commande getCommande() {
        return commande;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }

    public @NotNull LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public RetourBon setDateMtv(@NotNull LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public @NotNull AppUser getUser() {
        return user;
    }

    public RetourBon setUser(@NotNull AppUser user) {
        this.user = user;
        return this;
    }

    public @NotNull RetourStatut getStatut() {
        return statut;
    }

    public RetourBon setStatut(@NotNull RetourStatut statut) {
        this.statut = statut;
        return this;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public RetourBon setCommentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }

    public List<RetourBonItem> getRetourBonItems() {
        return retourBonItems;
    }

    public RetourBon setRetourBonItems(List<RetourBonItem> retourBonItems) {
        this.retourBonItems = retourBonItems;
        return this;
    }

    public List<ReponseRetourBon> getReponseRetourBons() {
        return reponseRetourBons;
    }

    public RetourBon setReponseRetourBons(List<ReponseRetourBon> reponseRetourBons) {
        this.reponseRetourBons = reponseRetourBons;
        return this;
    }

    public PharmaMlEnvoi getPharmamlEnvoi() {
        return pharmamlEnvoi;
    }

    public RetourBon setPharmamlEnvoi(PharmaMlEnvoi pharmamlEnvoi) {
        this.pharmamlEnvoi = pharmamlEnvoi;
        return this;
    }

    public Fournisseur getFournisseur() {
        return fournisseur;
    }

    public RetourBon setFournisseur(Fournisseur fournisseur) {
        this.fournisseur = fournisseur;
        return this;
    }

    public boolean isHorsCommande() {
        return horsCommande;
    }

    public RetourBon setHorsCommande(boolean horsCommande) {
        this.horsCommande = horsCommande;
        return this;
    }
}
