package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.NatureSubstance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** Une substance d'une spécialité, avec son dosage. Lecture seule. */
@Entity
@Table(name = "ref_specialite_composition")
public class RefSpecialiteComposition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cis", nullable = false)
    private RefSpecialite specialite;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "substance_code", nullable = false)
    private RefSubstance substance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dci_id")
    private RefDci dci;

    @Enumerated(EnumType.STRING)
    @Column(name = "nature", nullable = false, length = 2)
    private NatureSubstance nature;

    @Column(name = "dosage_texte", length = 120)
    private String dosageTexte;

    @Column(name = "dosage_valeur", precision = 18, scale = 6)
    private BigDecimal dosageValeur;

    @Column(name = "dosage_unite", length = 20)
    private String dosageUnite;

    @Column(name = "reference_dosage", length = 120)
    private String referenceDosage;

    /** Colonnes générées par la base : dosage ramené à une unité commune (mg pour les masses). */
    @Column(name = "unite_canon", insertable = false, updatable = false)
    private String uniteCanon;

    @Column(name = "dosage_canon", insertable = false, updatable = false, precision = 24, scale = 6)
    private BigDecimal dosageCanon;

    protected RefSpecialiteComposition() {}

    public Integer getId() {
        return id;
    }

    public RefSpecialite getSpecialite() {
        return specialite;
    }

    public RefSubstance getSubstance() {
        return substance;
    }

    public RefDci getDci() {
        return dci;
    }

    public NatureSubstance getNature() {
        return nature;
    }

    public String getDosageTexte() {
        return dosageTexte;
    }

    public BigDecimal getDosageValeur() {
        return dosageValeur;
    }

    public String getDosageUnite() {
        return dosageUnite;
    }

    public String getReferenceDosage() {
        return referenceDosage;
    }

    public String getUniteCanon() {
        return uniteCanon;
    }

    public BigDecimal getDosageCanon() {
        return dosageCanon;
    }
}
