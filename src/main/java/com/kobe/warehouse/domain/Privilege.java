package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "privilege")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Privilege implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @Size(max = 100)
    @Id
    @Column(length = 100)
    private String name;

    @NotNull
    private String libelle;

    @ManyToOne(optional = false)
    @NotNull
    private Menu menu;

    public @NotNull @Size(max = 100) String getName() {
        return name;
    }

    public Privilege setName(@NotNull @Size(max = 100) String name) {
        this.name = name;
        return this;
    }

    public @NotNull String getLibelle() {
        return libelle;
    }

    public Privilege setLibelle(@NotNull String libelle) {
        this.libelle = libelle;
        return this;
    }

    public @NotNull Menu getMenu() {
        return menu;
    }

    public Privilege setMenu(@NotNull Menu menu) {
        this.menu = menu;
        return this;
    }
}
