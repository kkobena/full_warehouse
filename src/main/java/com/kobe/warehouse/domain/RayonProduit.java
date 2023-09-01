package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

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
