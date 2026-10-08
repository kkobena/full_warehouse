package com.kobe.warehouse.domain.ordonnance;

import com.kobe.warehouse.domain.Produit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;

/** Ligne prescrite. Pas de DCI : les molécules se déduisent du produit, elles ne sont pas dupliquées. */
@Entity
@Table(name = "ordonnance_ligne")
public class OrdonnanceLigne implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ordonnance_id", nullable = false)
    private Ordonnance ordonnance;

    @Column(name = "rang", nullable = false)
    private short rang;

    /** Texte tel que lu ou saisi. */
    @Column(name = "texte_lu", length = 255)
    private String texteLu;

    /** Renseignée seulement pour une ligne proposée par l'OCR. */
    @Column(name = "confiance")
    private Short confiance;

    /** Produit prescrit ; le produit réellement délivré est celui de la ligne de vente liée. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    @Column(name = "posologie", length = 150)
    private String posologie;

    @Column(name = "duree_jours")
    private Integer dureeJours;

    @Column(name = "quantite_prescrite", nullable = false)
    private int quantitePrescrite;

    public Integer getId() {
        return id;
    }

    public Ordonnance getOrdonnance() {
        return ordonnance;
    }

    OrdonnanceLigne setOrdonnance(Ordonnance ordonnance) {
        this.ordonnance = ordonnance;
        return this;
    }

    public int getRang() {
        return rang;
    }

    public OrdonnanceLigne setRang(int rang) {
        this.rang = (short) rang;
        return this;
    }

    public String getTexteLu() {
        return texteLu;
    }

    public OrdonnanceLigne setTexteLu(String texteLu) {
        this.texteLu = texteLu;
        return this;
    }

    public Short getConfiance() {
        return confiance;
    }

    public Produit getProduit() {
        return produit;
    }

    public OrdonnanceLigne setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public String getPosologie() {
        return posologie;
    }

    public OrdonnanceLigne setPosologie(String posologie) {
        this.posologie = posologie;
        return this;
    }

    public Integer getDureeJours() {
        return dureeJours;
    }

    public OrdonnanceLigne setDureeJours(Integer dureeJours) {
        this.dureeJours = dureeJours;
        return this;
    }

    public int getQuantitePrescrite() {
        return quantitePrescrite;
    }

    public OrdonnanceLigne setQuantitePrescrite(int quantitePrescrite) {
        this.quantitePrescrite = quantitePrescrite;
        return this;
    }
}
