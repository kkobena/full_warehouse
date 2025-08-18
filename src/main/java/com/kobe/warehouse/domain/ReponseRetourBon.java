package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.RetourBonStatut;
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
@Table(name = "reponse_retour_bon")
public class ReponseRetourBon implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "date_mtv", nullable = false)
    private LocalDateTime dateMtv = LocalDateTime.now();

    @NotNull
    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate;

    @ManyToOne(optional = false)
    @NotNull
    private User user;

    @ManyToOne(optional = false)
    @NotNull
    private RetourBon retourBon;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 1)
    private RetourBonStatut statut = RetourBonStatut.PROCESSING;

    @Column(name = "commentaire", length = 150)
    private String commentaire;



    @OneToMany(mappedBy = "reponseRetourBon")
    private List<ReponseRetourBonItem> reponseRetourBonItems = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public ReponseRetourBon setId(Long id) {
        this.id = id;
        return this;
    }

    public @NotNull LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public ReponseRetourBon setDateMtv(@NotNull LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public @NotNull LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public ReponseRetourBon setModifiedDate(@NotNull LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
        return this;
    }

    public @NotNull User getUser() {
        return user;
    }

    public ReponseRetourBon setUser(@NotNull User user) {
        this.user = user;
        return this;
    }

    public @NotNull RetourBon getRetourBon() {
        return retourBon;
    }

    public ReponseRetourBon setRetourBon(@NotNull RetourBon retourBon) {
        this.retourBon = retourBon;
        return this;
    }

    public @NotNull RetourBonStatut getStatut() {
        return statut;
    }

    public ReponseRetourBon setStatut(@NotNull RetourBonStatut statut) {
        this.statut = statut;
        return this;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public ReponseRetourBon setCommentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }



    public List<ReponseRetourBonItem> getReponseRetourBonItems() {
        return reponseRetourBonItems;
    }

    public ReponseRetourBon setReponseRetourBonItems(List<ReponseRetourBonItem> reponseRetourBonItems) {
        this.reponseRetourBonItems = reponseRetourBonItems;
        return this;
    }
}
