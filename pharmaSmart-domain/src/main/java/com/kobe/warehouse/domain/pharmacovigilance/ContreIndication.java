package com.kobe.warehouse.domain.pharmacovigilance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** Molécule x critère patient (âge, sexe, grossesse, allaitement). */
@Entity
@Table(name = "contre_indication")
public class ContreIndication implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "version_id", nullable = false)
    private Integer versionId;

    @Column(name = "ref_dci_id", nullable = false)
    private Integer refDciId;

    @Enumerated(EnumType.STRING)
    @Column(name = "critere", nullable = false)
    private CritereContreIndication critere;

    @Column(name = "valeur_num")
    private BigDecimal valeurNum;

    @Column(name = "valeur_texte")
    private String valeurTexte;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau", nullable = false)
    private NiveauInteraction niveau;

    @Column(name = "motif")
    private String motif;

    @Column(name = "conduite")
    private String conduite;

    protected ContreIndication() {}

    public ContreIndication(
        Integer versionId,
        Integer refDciId,
        CritereContreIndication critere,
        BigDecimal valeurNum,
        String valeurTexte,
        NiveauInteraction niveau,
        String motif,
        String conduite
    ) {
        this.versionId = versionId;
        this.refDciId = refDciId;
        this.critere = critere;
        this.valeurNum = valeurNum;
        this.valeurTexte = valeurTexte;
        this.niveau = niveau;
        this.motif = motif;
        this.conduite = conduite;
    }

    public Integer getId() {
        return id;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public Integer getRefDciId() {
        return refDciId;
    }

    public CritereContreIndication getCritere() {
        return critere;
    }

    public BigDecimal getValeurNum() {
        return valeurNum;
    }

    public String getValeurTexte() {
        return valeurTexte;
    }

    public NiveauInteraction getNiveau() {
        return niveau;
    }

    public String getMotif() {
        return motif;
    }

    public String getConduite() {
        return conduite;
    }
}
