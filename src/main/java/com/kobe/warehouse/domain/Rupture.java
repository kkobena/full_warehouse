package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "retour_bon")
public class Rupture implements Serializable {

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
    private Produit produit;

    @ManyToOne(optional = false)
    @NotNull
    private Fournisseur fournisseur;

    @Column(name = "qty", nullable = false)
    private int qty;

    private boolean productStillOutOfStock = true;

    public boolean isProductStillOutOfStock() {
        return productStillOutOfStock;
    }

    public Rupture setProductStillOutOfStock(boolean productStillOutOfStock) {
        this.productStillOutOfStock = productStillOutOfStock;
        return this;
    }

    public LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public Rupture setDateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public Fournisseur getFournisseur() {
        return fournisseur;
    }

    public Rupture setFournisseur(Fournisseur fournisseur) {
        this.fournisseur = fournisseur;
        return this;
    }

    public Long getId() {
        return id;
    }

    public Rupture setId(Long id) {
        this.id = id;
        return this;
    }

    public Produit getProduit() {
        return produit;
    }

    public Rupture setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public int getQty() {
        return qty;
    }

    public Rupture setQty(int qty) {
        this.qty = qty;
        return this;
    }
}
