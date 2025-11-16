package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.AjustementStatut;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ajust")
public class Ajust implements Serializable {

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
    @Column(name = "statut", nullable = false, length = 8)
    private AjustementStatut statut = AjustementStatut.PENDING;

    @ManyToOne(optional = false)
    @NotNull
    private Storage storage;

    @Column(name = "commentaire")
    private String commentaire;

    @OneToMany(mappedBy = "ajust", cascade = { CascadeType.REMOVE })
    private List<Ajustement> ajustements = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<Ajustement> getAjustements() {
        return ajustements;
    }

    public Ajust setAjustements(List<Ajustement> ajustements) {
        this.ajustements = ajustements;
        return this;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public Ajust setCommentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }

    public @NotNull Storage getStorage() {
        return storage;
    }

    public Ajust setStorage(Storage storage) {
        this.storage = storage;
        return this;
    }

    public @NotNull AjustementStatut getStatut() {
        return statut;
    }

    public void setStatut(AjustementStatut statut) {
        this.statut = statut;
    }

    public @NotNull AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public @NotNull LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public void setDateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
    }
}
