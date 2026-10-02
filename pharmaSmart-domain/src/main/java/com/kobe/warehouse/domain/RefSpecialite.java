package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TypeGenerique;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;

/** Spécialité pharmaceutique du référentiel BDPM (code CIS). */
@Entity
@Table(name = "ref_specialite")
public class RefSpecialite implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "cis", length = 8)
    private String cis;

    @Column(name = "libelle", nullable = false)
    private String libelle;

    /** Colonne générée par la base (ref_normaliser). */
    @Column(name = "libelle_normalise", insertable = false, updatable = false)
    private String libelleNormalise;

    @Column(name = "forme", length = 120)
    private String forme;

    @Column(name = "voies", length = 150)
    private String voies;

    @Column(name = "titulaire", length = 100)
    private String titulaire;

    @Column(name = "commercialisee", nullable = false)
    private boolean commercialisee = true;

    @Column(name = "composition", columnDefinition = "text")
    private String composition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupe_generique_id")
    private RefGroupeGenerique groupeGenerique;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_generique", length = 40)
    private TypeGenerique typeGenerique;

    @Column(name = "princeps_du_groupe")
    private String princepsDuGroupe;

    protected RefSpecialite() {}

    public String getCis() {
        return cis;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getLibelleNormalise() {
        return libelleNormalise;
    }

    public String getForme() {
        return forme;
    }

    public String getVoies() {
        return voies;
    }

    public String getTitulaire() {
        return titulaire;
    }

    public boolean isCommercialisee() {
        return commercialisee;
    }

    public String getComposition() {
        return composition;
    }

    public RefGroupeGenerique getGroupeGenerique() {
        return groupeGenerique;
    }

    public TypeGenerique getTypeGenerique() {
        return typeGenerique;
    }

    public String getPrincepsDuGroupe() {
        return princepsDuGroupe;
    }
}
