package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Table(name = "rayon_produit", uniqueConstraints = { @UniqueConstraint(columnNames = { "produit_id", "rayon_id" })
    })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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
    private Rayon  rayon;

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

    public RayonProduit() {
    }
}
