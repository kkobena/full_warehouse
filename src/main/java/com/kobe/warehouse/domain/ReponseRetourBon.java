package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    private Integer id;

    @NotNull
    @Column(name = "date_mtv", nullable = false)
    private LocalDateTime dateMtv = LocalDateTime.now();

    @ManyToOne(optional = false)
    @NotNull
    private AppUser user;

    @ManyToOne(optional = false)
    @NotNull
    private RetourBon retourBon;

    @OneToMany(mappedBy = "reponseRetourBon")
    private List<ReponseRetourBonItem> reponseRetourBonItems = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public ReponseRetourBon setId(Integer id) {
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

    public @NotNull AppUser getUser() {
        return user;
    }

    public ReponseRetourBon setUser(@NotNull AppUser user) {
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

    public List<ReponseRetourBonItem> getReponseRetourBonItems() {
        return reponseRetourBonItems;
    }

    public ReponseRetourBon setReponseRetourBonItems(List<ReponseRetourBonItem> reponseRetourBonItems) {
        this.reponseRetourBonItems = reponseRetourBonItems;
        return this;
    }
}
