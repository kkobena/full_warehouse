package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.RetourStatut;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    @Column(name = "statut", nullable = false,length = 15)
    private RetourStatut statut = RetourStatut.VALIDATED;

    @Column(name = "commentaire", length = 150)
    private String commentaire;

    @OneToMany(mappedBy = "retourBon")
    private List<RetourBonItem> retourBonItems = new ArrayList<>();

    @ManyToOne(optional = false)
    @NotNull
    private Commande commande;

    @OneToMany(mappedBy = "retourBon")
    private List<ReponseRetourBon> reponseRetourBons = new ArrayList<>();

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
}
