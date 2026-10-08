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

/**
 * Couple (molécule ou classe) x (molécule ou classe). Pour chaque côté, exactement une des deux
 * colonnes est renseignée (contrainte en base).
 */
@Entity
@Table(name = "interaction")
public class Interaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "version_id", nullable = false)
    private Integer versionId;

    @Column(name = "a_ref_dci_id")
    private Integer aRefDciId;

    @Column(name = "a_classe_id")
    private Integer aClasseId;

    @Column(name = "b_ref_dci_id")
    private Integer bRefDciId;

    @Column(name = "b_classe_id")
    private Integer bClasseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau", nullable = false)
    private NiveauInteraction niveau;

    @Column(name = "mecanisme")
    private String mecanisme;

    @Column(name = "conduite")
    private String conduite;

    protected Interaction() {}

    public Interaction(
        Integer versionId,
        Integer aRefDciId,
        Integer aClasseId,
        Integer bRefDciId,
        Integer bClasseId,
        NiveauInteraction niveau,
        String mecanisme,
        String conduite
    ) {
        this.versionId = versionId;
        this.aRefDciId = aRefDciId;
        this.aClasseId = aClasseId;
        this.bRefDciId = bRefDciId;
        this.bClasseId = bClasseId;
        this.niveau = niveau;
        this.mecanisme = mecanisme;
        this.conduite = conduite;
    }

    public Integer getId() {
        return id;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public Integer getARefDciId() {
        return aRefDciId;
    }

    public Integer getAClasseId() {
        return aClasseId;
    }

    public Integer getBRefDciId() {
        return bRefDciId;
    }

    public Integer getBClasseId() {
        return bClasseId;
    }

    public NiveauInteraction getNiveau() {
        return niveau;
    }

    public String getMecanisme() {
        return mecanisme;
    }

    public String getConduite() {
        return conduite;
    }
}
