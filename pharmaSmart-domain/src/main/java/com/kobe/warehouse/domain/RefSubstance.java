package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;

/** Substance du référentiel BDPM (code substance national). Lecture seule. */
@Entity
@Table(name = "ref_substance")
public class RefSubstance implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "code", length = 10)
    private String code;

    @Column(name = "libelle", nullable = false)
    private String libelle;

    /** Colonne générée par la base (ref_normaliser). */
    @Column(name = "libelle_normalise", insertable = false, updatable = false)
    private String libelleNormalise;

    protected RefSubstance() {}

    public String getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getLibelleNormalise() {
        return libelleNormalise;
    }
}
