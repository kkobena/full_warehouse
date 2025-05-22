package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.StatutAvoir;
import jakarta.persistence.CascadeType;
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
import java.util.Objects;

@Entity
@Table(name = "avoir")
public class Avoir implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "date_mtv", nullable = false)
    private LocalDateTime dateMtv = LocalDateTime.now();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutAvoir statut = StatutAvoir.EN_COURS;

    @ManyToOne(optional = false)
    @NotNull
    private User user;

    @OneToMany(mappedBy = "avoir", cascade = { CascadeType.REMOVE, CascadeType.PERSIST })
    private List<LigneAvoir> ligneAvoirs = new ArrayList<>();

    @ManyToOne(optional = false)
    @NotNull
    private WarehouseCalendar calendar;

    public WarehouseCalendar getCalendar() {
        return calendar;
    }

    public Avoir setCalendar(WarehouseCalendar calendar) {
        this.calendar = calendar;
        return this;
    }

    public Long getId() {
        return id;
    }

    public Avoir setId(Long id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public Avoir setDateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public StatutAvoir getStatut() {
        return statut;
    }

    public Avoir setStatut(StatutAvoir statut) {
        this.statut = statut;
        return this;
    }

    public User getUser() {
        return user;
    }

    public Avoir setUser(User user) {
        this.user = user;
        return this;
    }

    public List<LigneAvoir> getLigneAvoirs() {
        return ligneAvoirs;
    }

    public Avoir setLigneAvoirs(List<LigneAvoir> ligneAvoirs) {
        this.ligneAvoirs = ligneAvoirs;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Avoir avoir = (Avoir) o;
        return Objects.equals(id, avoir.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
