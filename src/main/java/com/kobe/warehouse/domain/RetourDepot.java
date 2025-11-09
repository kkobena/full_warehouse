package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.RetourStatut;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "retour_depot")
public class RetourDepot implements Serializable {

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
    private AppUser user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_retour", nullable = false, length = 15)
    private RetourStatut statut = RetourStatut.PROCESSING;
    @OneToMany(mappedBy = "retourBon")
    private List<RetourBonItem> retourBonItems = new ArrayList<>();

    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "vente_depot_id", referencedColumnName = "id"),
        @JoinColumn(name = "vente_depot_date", referencedColumnName = "sale_date")
    })
    private VenteDepot venteDepot;


    @ManyToOne(optional = false)
    @NotNull
    private Magasin  depot;

    public Long getId() {
        return id;
    }

    public RetourDepot setId(Long id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public RetourDepot setDateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public AppUser getUser() {
        return user;
    }

    public RetourDepot setUser(AppUser user) {
        this.user = user;
        return this;
    }

    public RetourStatut getStatut() {
        return statut;
    }

    public RetourDepot setStatut(RetourStatut statut) {
        this.statut = statut;
        return this;
    }

    public List<RetourBonItem> getRetourBonItems() {
        return retourBonItems;
    }

    public RetourDepot setRetourBonItems(List<RetourBonItem> retourBonItems) {
        this.retourBonItems = retourBonItems;
        return this;
    }

    public VenteDepot getVenteDepot() {
        return venteDepot;
    }

    public RetourDepot setVenteDepot(VenteDepot venteDepot) {
        this.venteDepot = venteDepot;
        return this;
    }

    public Magasin getDepot() {
        return depot;
    }

    public RetourDepot setDepot(Magasin depot) {
        this.depot = depot;
        return this;
    }
}
