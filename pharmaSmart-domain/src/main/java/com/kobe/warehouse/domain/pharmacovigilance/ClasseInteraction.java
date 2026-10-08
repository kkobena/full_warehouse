package com.kobe.warehouse.domain.pharmacovigilance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;

/** Classe du thésaurus (« AINS », « anticoagulants oraux »…), propre à une version. */
@Entity
@Table(name = "classe_interaction")
public class ClasseInteraction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "version_id", nullable = false)
    private Integer versionId;

    @Column(name = "libelle", nullable = false)
    private String libelle;

    protected ClasseInteraction() {}

    public ClasseInteraction(Integer versionId, String libelle) {
        this.versionId = versionId;
        this.libelle = libelle;
    }

    public Integer getId() {
        return id;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public String getLibelle() {
        return libelle;
    }
}
