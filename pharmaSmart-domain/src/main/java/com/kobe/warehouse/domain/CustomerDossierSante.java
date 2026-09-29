package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Dossier de sécurité d'un client : pathologies, grossesse, allaitement, poids (fiche client, lot 2). */
@Entity
@Table(name = "customer_dossier_sante")
public class CustomerDossierSante implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "customer_id")
    private Integer customerId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pathologies", nullable = false, columnDefinition = "jsonb")
    private List<String> pathologies = new ArrayList<>();

    @Column(name = "grossesse", nullable = false)
    private boolean grossesse;

    @Column(name = "date_terme")
    private LocalDate dateTerme;

    @Column(name = "allaitement", nullable = false)
    private boolean allaitement;

    @Column(name = "poids_kg", precision = 5, scale = 2)
    private BigDecimal poidsKg;

    @Column(name = "date_pesee")
    private LocalDate datePesee;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private AppUser updatedBy;

    public Integer getCustomerId() {
        return customerId;
    }

    public CustomerDossierSante setCustomerId(Integer customerId) {
        this.customerId = customerId;
        return this;
    }

    public List<String> getPathologies() {
        return pathologies;
    }

    public CustomerDossierSante setPathologies(List<String> pathologies) {
        this.pathologies = pathologies;
        return this;
    }

    public boolean isGrossesse() {
        return grossesse;
    }

    public CustomerDossierSante setGrossesse(boolean grossesse) {
        this.grossesse = grossesse;
        return this;
    }

    public LocalDate getDateTerme() {
        return dateTerme;
    }

    public CustomerDossierSante setDateTerme(LocalDate dateTerme) {
        this.dateTerme = dateTerme;
        return this;
    }

    public boolean isAllaitement() {
        return allaitement;
    }

    public CustomerDossierSante setAllaitement(boolean allaitement) {
        this.allaitement = allaitement;
        return this;
    }

    public BigDecimal getPoidsKg() {
        return poidsKg;
    }

    public CustomerDossierSante setPoidsKg(BigDecimal poidsKg) {
        this.poidsKg = poidsKg;
        return this;
    }

    public LocalDate getDatePesee() {
        return datePesee;
    }

    public CustomerDossierSante setDatePesee(LocalDate datePesee) {
        this.datePesee = datePesee;
        return this;
    }

    public String getNote() {
        return note;
    }

    public CustomerDossierSante setNote(String note) {
        this.note = note;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public CustomerDossierSante setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public AppUser getUpdatedBy() {
        return updatedBy;
    }

    public CustomerDossierSante setUpdatedBy(AppUser updatedBy) {
        this.updatedBy = updatedBy;
        return this;
    }
}
