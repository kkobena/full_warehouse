package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Entity
@Table(name = "ajust")
public class Ajust implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Column(name = "date_mtv", nullable = false)
    private LocalDateTime dateMtv = LocalDateTime.now();

    @ManyToOne(optional = false)
    @NotNull
    private User user;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "statut", nullable = false)
    private SalesStatut statut = SalesStatut.PENDING;
    @ManyToOne(optional = false)
    @NotNull
    private Storage storage;
    @Column(name = "commentaire")
    private String commentaire;

    public void setId(Long id) {
        this.id = id;
    }

    public void setStatut(SalesStatut statut) {
        this.statut = statut;
    }

    public Ajust setCommentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }

    public void setDateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
    }

   

    public void setUser(User user) {
        this.user = user;
    }

    public Ajust setStorage(Storage storage) {
        this.storage = storage;
        return this;
    }
}
