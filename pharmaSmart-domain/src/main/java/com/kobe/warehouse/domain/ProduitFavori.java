package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Produit épinglé à la grille de vente du comptoir d'un magasin ; {@code ordre} règle la place de sa tuile. */
@Entity
@Table(name = "produit_favori", uniqueConstraints = { @UniqueConstraint(columnNames = { "magasin_id", "produit_id" }) })
public class ProduitFavori implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Magasin magasin;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Produit produit;

    @Column(name = "ordre", nullable = false)
    private int ordre;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Integer getId() {
        return id;
    }

    public Magasin getMagasin() {
        return magasin;
    }

    public ProduitFavori setMagasin(Magasin magasin) {
        this.magasin = magasin;
        return this;
    }

    public Produit getProduit() {
        return produit;
    }

    public ProduitFavori setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public int getOrdre() {
        return ordre;
    }

    public ProduitFavori setOrdre(int ordre) {
        this.ordre = ordre;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
