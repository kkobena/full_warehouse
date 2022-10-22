package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.SalesStatut;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "ajust")
public class Ajust implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Column(name = "date_mtv", nullable = false)
    private Instant dateMtv=Instant.now();
    @ManyToOne(optional = false)
    @NotNull
    private DateDimension dateDimension;
    @ManyToOne(optional = false)
    @NotNull
    private User user;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "statut", nullable = false)
    private SalesStatut statut=SalesStatut.PENDING;
    @ManyToOne(optional = false)
    @NotNull
    private Storage storage;
    @Column(name = "commentaire")
    private String commentaire;
    public Long getId() {
        return id;
    }

    public SalesStatut getStatut() {
        return statut;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public Ajust setCommentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }

    public void setStatut(SalesStatut statut) {
        this.statut = statut;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getDateMtv() {
        return dateMtv;
    }

    public void setDateMtv(Instant dateMtv) {
        this.dateMtv = dateMtv;
    }

    public DateDimension getDateDimension() {
        return dateDimension;
    }

    public void setDateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
    }

    public User getUser() {
        return user;
    }

    public Storage getStorage() {
        return storage;
    }

    public Ajust setStorage(Storage storage) {
        this.storage = storage;
        return this;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
