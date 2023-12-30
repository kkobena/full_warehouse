package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "rayon_produit", uniqueConstraints = {@UniqueConstraint(columnNames = {"produit_id", "rayon_id"})
})
public class RayonProduit implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = "rayonProduits", allowSetters = true)
    private Produit produit;
    @ManyToOne(optional = false)
    @NotNull
    private Rayon rayon;

    public RayonProduit() {
    }

    public Long getId() {
        return id;
    }

    public RayonProduit setId(Long id) {
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
