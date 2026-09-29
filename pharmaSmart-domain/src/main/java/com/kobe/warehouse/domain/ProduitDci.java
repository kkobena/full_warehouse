package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** Molécule portée par un produit ; le rang 1 est la molécule principale. */
@Entity
@Table(name = "produit_dci")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ProduitDci implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Produit produit;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Dci dci;

    @Min(1)
    @Column(name = "rang", nullable = false)
    private short rang = 1;

    @Column(name = "dosage_valeur", precision = 12, scale = 4)
    private BigDecimal dosageValeur;

    @Column(name = "dosage_unite", length = 10)
    private String dosageUnite;

    public ProduitDci() {}

    public ProduitDci(Produit produit, Dci dci, int rang) {
        this.produit = produit;
        this.dci = dci;
        this.rang = (short) rang;
    }

    public Integer getId() {
        return id;
    }

    public ProduitDci setId(Integer id) {
        this.id = id;
        return this;
    }

    public Produit getProduit() {
        return produit;
    }

    public ProduitDci setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public Dci getDci() {
        return dci;
    }

    public ProduitDci setDci(Dci dci) {
        this.dci = dci;
        return this;
    }

    public int getRang() {
        return rang;
    }

    public ProduitDci setRang(int rang) {
        this.rang = (short) rang;
        return this;
    }

    public BigDecimal getDosageValeur() {
        return dosageValeur;
    }

    public ProduitDci setDosageValeur(BigDecimal dosageValeur) {
        this.dosageValeur = dosageValeur;
        return this;
    }

    public String getDosageUnite() {
        return dosageUnite;
    }

    public ProduitDci setDosageUnite(String dosageUnite) {
        this.dosageUnite = dosageUnite;
        return this;
    }
}
