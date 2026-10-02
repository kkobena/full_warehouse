package com.kobe.warehouse.domain;

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

/**
 * Dénomination commune du référentiel BDPM. {@code dci} la relie à la DCI du catalogue ; le lien
 * est posé par la fonction SQL {@code ref_lier_dci}, jamais par l'application.
 */
@Entity
@Table(name = "ref_dci")
public class RefDci implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "libelle", nullable = false)
    private String libelle;

    /** Colonne générée par la base (ref_normaliser). */
    @Column(name = "libelle_normalise", insertable = false, updatable = false)
    private String libelleNormalise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dci_id", insertable = false, updatable = false)
    private Dci dci;

    protected RefDci() {}

    public Integer getId() {
        return id;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getLibelleNormalise() {
        return libelleNormalise;
    }

    public Dci getDci() {
        return dci;
    }
}
