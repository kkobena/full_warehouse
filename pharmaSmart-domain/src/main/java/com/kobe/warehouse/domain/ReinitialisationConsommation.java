package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Consommation effacée par une facture définitive (plafond non absolu), rendue si la facture est annulée. */
@Entity
@Table(name = "reinitialisation_consommation")
public class ReinitialisationConsommation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "facture_id", nullable = false)
    private Long factureId;

    @Column(name = "facture_date", nullable = false)
    private LocalDate factureDate;

    @Column(name = "client_tiers_payant_id")
    private Integer clientTiersPayantId;

    @Column(name = "tiers_payant_id")
    private Integer tiersPayantId;

    @Column(name = "montant", nullable = false)
    private long montant;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Integer getId() {
        return id;
    }

    public Long getFactureId() {
        return factureId;
    }

    public ReinitialisationConsommation setFactureId(Long factureId) {
        this.factureId = factureId;
        return this;
    }

    public LocalDate getFactureDate() {
        return factureDate;
    }

    public ReinitialisationConsommation setFactureDate(LocalDate factureDate) {
        this.factureDate = factureDate;
        return this;
    }

    public Integer getClientTiersPayantId() {
        return clientTiersPayantId;
    }

    public ReinitialisationConsommation setClientTiersPayantId(Integer clientTiersPayantId) {
        this.clientTiersPayantId = clientTiersPayantId;
        return this;
    }

    public Integer getTiersPayantId() {
        return tiersPayantId;
    }

    public ReinitialisationConsommation setTiersPayantId(Integer tiersPayantId) {
        this.tiersPayantId = tiersPayantId;
        return this;
    }

    public long getMontant() {
        return montant;
    }

    public ReinitialisationConsommation setMontant(long montant) {
        this.montant = montant;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
