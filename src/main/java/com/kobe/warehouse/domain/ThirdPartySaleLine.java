package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.data.domain.Persistable;

@Entity
@IdClass(AssuranceSaleId.class)
@Table(name = "third_party_sale_line", indexes = {@Index(columnList = "num_bon", name = "third_party_sale_line_num_bon")}, uniqueConstraints = {@UniqueConstraint(columnNames = {"client_tiers_payant_id", "sale_id", "sale_date"})})
public class ThirdPartySaleLine implements Persistable<AssuranceSaleId>, Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;
    @Id
    private Long id;
    @Id
    @Column(name = "sale_date")
    private LocalDate saleDate = LocalDate.now();

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumns({
        @JoinColumn(name = "sale_id", referencedColumnName = "id"),
        @JoinColumn(name = "sale_sale_date", referencedColumnName = "sale_date")
    })
    private ThirdPartySales sale;

    @Column(name = "num_bon", length = 50)
    private String numBon;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "client_tiers_payant_id", referencedColumnName = "id")
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

    @Column(name = "montant_regle")
    private Integer montantRegle = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "facture_tiers_payant_id", referencedColumnName = "id"),
        @JoinColumn(name = "invoice_date", referencedColumnName = "invoice_date")
    })
    private FactureTiersPayant factureTiersPayant;
    @Transient
    private boolean isNew = true;
    public AssuranceSaleId getId() {
        return new AssuranceSaleId(id, saleDate);
    }

    public ThirdPartySaleLine setId(Long id) {
        this.id = id;
        return this;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public @NotNull ThirdPartySales getSale() {
        return sale;
    }

    public ThirdPartySaleLine setSale(ThirdPartySales sale) {
        this.sale = sale;
        return this;
    }

    public String getNumBon() {
        return numBon;
    }

    public ThirdPartySaleLine setNumBon(String numBon) {
        this.numBon = numBon;
        return this;
    }

    public @NotNull ClientTiersPayant getClientTiersPayant() {
        return clientTiersPayant;
    }

    public ThirdPartySaleLine setClientTiersPayant(ClientTiersPayant clientTiersPayant) {
        this.clientTiersPayant = clientTiersPayant;
        return this;
    }

    public @NotNull Integer getMontant() {
        return montant;
    }

    public ThirdPartySaleLine setMontant(Integer montant) {
        this.montant = montant;
        return this;
    }

    public @NotNull LocalDateTime getCreated() {
        return created;
    }

    public ThirdPartySaleLine setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public @NotNull LocalDateTime getUpdated() {
        return updated;
    }

    public ThirdPartySaleLine setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public @NotNull LocalDateTime getEffectiveUpdateDate() {
        return effectiveUpdateDate;
    }

    public ThirdPartySaleLine setEffectiveUpdateDate(LocalDateTime effectiveUpdateDate) {
        this.effectiveUpdateDate = effectiveUpdateDate;
        return this;
    }

    public @NotNull ThirdPartySaleStatut getStatut() {
        return statut;
    }

    public ThirdPartySaleLine setStatut(ThirdPartySaleStatut statut) {
        this.statut = statut;
        return this;
    }

    @NotNull
    public short getTaux() {
        return taux;
    }

    public ThirdPartySaleLine setTaux(short taux) {
        this.taux = taux;
        return this;
    }

    public FactureTiersPayant getFactureTiersPayant() {
        return factureTiersPayant;
    }

    public ThirdPartySaleLine setFactureTiersPayant(FactureTiersPayant factureTiersPayant) {
        this.factureTiersPayant = factureTiersPayant;
        return this;
    }

    public Integer getMontantRegle() {
        return montantRegle;
    }

    public ThirdPartySaleLine setMontantRegle(Integer montantRegle) {
        this.montantRegle = montantRegle;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ThirdPartySaleLine that = (ThirdPartySaleLine) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PrePersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }
}
