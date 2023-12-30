package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Entity
@Table(
    name = "third_party_saleLine",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"client_tiers_payant_id", "sale_id"})})
public class ThirdPartySaleLine implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    private ThirdPartySales sale;

    @Column(name = "num_bon", length = 50)
    private String numBon;

    @NotNull
    @ManyToOne(optional = false)
    private ClientTiersPayant clientTiersPayant;

    @NotNull
    @Column(name = "montant", nullable = false)
    private Integer montant;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime created;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updated = LocalDateTime.now();

    @NotNull
    @Column(name = "effective_update_date", nullable = false)
    private LocalDateTime effectiveUpdateDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private ThirdPartySaleStatut statut = ThirdPartySaleStatut.ACTIF;

    @NotNull
    @Column(name = "taux", nullable = false)
    private short taux;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_statut", nullable = false)
    private ThirdPartySaleStatut invoiceStatut = ThirdPartySaleStatut.ACTIF;

  public ThirdPartySaleLine setTaux(short taux) {
        this.taux = taux;
        return this;
    }

  public ThirdPartySaleLine setInvoiceStatut(ThirdPartySaleStatut invoiceStatut) {
        this.invoiceStatut = invoiceStatut;
        return this;
    }

  public ThirdPartySaleLine setId(Long id) {
        this.id = id;
        return this;
    }

  public ThirdPartySaleLine setSale(ThirdPartySales sale) {
        this.sale = sale;
        return this;
    }

  public ThirdPartySaleLine setNumBon(String numBon) {
        this.numBon = numBon;
        return this;
    }

  public ThirdPartySaleLine setClientTiersPayant(ClientTiersPayant clientTiersPayant) {
        this.clientTiersPayant = clientTiersPayant;
        return this;
    }

  public ThirdPartySaleLine setMontant(Integer montant) {
        this.montant = montant;
        return this;
    }

  public ThirdPartySaleLine setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

  public ThirdPartySaleLine setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

  public ThirdPartySaleLine setEffectiveUpdateDate(LocalDateTime effectiveUpdateDate) {
        this.effectiveUpdateDate = effectiveUpdateDate;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThirdPartySaleLine that = (ThirdPartySaleLine) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

  public ThirdPartySaleLine setStatut(ThirdPartySaleStatut statut) {
        this.statut = statut;
        return this;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }
}
