package com.kobe.warehouse.domain.ordonnance;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/** Index d'en-tête : « cette vente a servi l'ordonnance du Dr X du 12/09 ». */
@Entity
@Table(name = "ordonnance_vente")
public class OrdonnanceVente implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private Id id;

    protected OrdonnanceVente() {}

    public OrdonnanceVente(Integer ordonnanceId, Long salesId, LocalDate salesDate) {
        this.id = new Id(ordonnanceId, salesId, salesDate);
    }

    public Id getId() {
        return id;
    }

    @Embeddable
    public record Id(
        @Column(name = "ordonnance_id") Integer ordonnanceId,
        @Column(name = "sales_id") Long salesId,
        @Column(name = "sales_date") LocalDate salesDate
    ) implements Serializable {}
}
