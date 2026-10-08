package com.kobe.warehouse.domain.pharmacovigilance;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;

/** Appartenance d'une molécule du référentiel à une classe d'interaction. */
@Entity
@Table(name = "dci_classe")
public class DciClasse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private Id id;

    protected DciClasse() {}

    public DciClasse(Integer classeId, Integer refDciId) {
        this.id = new Id(classeId, refDciId);
    }

    public Id getId() {
        return id;
    }

    @Embeddable
    public record Id(@Column(name = "classe_id") Integer classeId, @Column(name = "ref_dci_id") Integer refDciId)
        implements Serializable {}
}
