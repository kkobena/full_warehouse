package com.kobe.warehouse.domain.ordonnance;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/** Prescripteur déclaré sur une vente sans ordonnance saisie (produit sur ordonnance). */
@Entity
@Table(name = "vente_prescripteur")
public class VentePrescripteur implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private Id id;

    @Column(name = "prescripteur_id", nullable = false)
    private Integer prescripteurId;

    @Column(name = "created_by_id")
    private Integer createdById;

    protected VentePrescripteur() {}

    public VentePrescripteur(Long salesId, LocalDate salesDate, Integer prescripteurId, Integer createdById) {
        this.id = new Id(salesId, salesDate);
        this.prescripteurId = prescripteurId;
        this.createdById = createdById;
    }

    public Id getId() {
        return id;
    }

    public Integer getPrescripteurId() {
        return prescripteurId;
    }

    public VentePrescripteur setPrescripteurId(Integer prescripteurId) {
        this.prescripteurId = prescripteurId;
        return this;
    }

    @Embeddable
    public record Id(@Column(name = "sales_id") Long salesId, @Column(name = "sales_date") LocalDate salesDate) implements Serializable {}
}
