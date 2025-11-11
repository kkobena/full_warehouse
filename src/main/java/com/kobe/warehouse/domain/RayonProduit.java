package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "rayon_produit", uniqueConstraints = { @UniqueConstraint(columnNames = { "produit_id", "rayon_id" }) })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class RayonProduit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    @NotNull
    @JsonIgnoreProperties(value = "rayonProduits", allowSetters = true)
    private Produit produit;

    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    @NotNull
    private Rayon rayon;

    public RayonProduit() {}

    public Integer getId() {
        return id;
    }

    public RayonProduit setId(Integer id) {
        this.id = id;
        return this;
    }

    public Produit getProduit() {
        return produit;
    }

    public RayonProduit setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public Rayon getRayon() {
        return rayon;
    }

    public RayonProduit setRayon(Rayon rayon) {
        this.rayon = rayon;
        return this;
    }
}
