package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Traitement chronique d'un patient. Déclaré par molécule ({@link Dci}) : toute délivrance d'un
 * produit qui la contient, générique ou princeps, le renouvelle. Un produit imposé le restreint à
 * ce seul produit, pour un patient non substituable.
 */
@Entity
@Table(name = "customer_traitement_chronique")
public class CustomerTraitementChronique implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dci_id")
    private Dci dci;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    @Column(name = "dosage", length = 50)
    private String dosage;

    @Column(name = "posologie", length = 150)
    private String posologie;

    /** Jours couverts par une délivrance : l'échéance est la dernière délivrance plus cette durée. */
    @Column(name = "duree_jours", nullable = false)
    private int dureeJours;

    @Column(name = "date_ordonnance")
    private LocalDate dateOrdonnance;

    @Column(name = "date_fin_ordonnance")
    private LocalDate dateFinOrdonnance;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private AppUser updatedBy;

    public Integer getId() {
        return id;
    }

    public CustomerTraitementChronique setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public CustomerTraitementChronique setCustomerId(Integer customerId) {
        this.customerId = customerId;
        return this;
    }

    public Dci getDci() {
        return dci;
    }

    public CustomerTraitementChronique setDci(Dci dci) {
        this.dci = dci;
        return this;
    }

    public Produit getProduit() {
        return produit;
    }

    public CustomerTraitementChronique setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public String getDosage() {
        return dosage;
    }

    public CustomerTraitementChronique setDosage(String dosage) {
        this.dosage = dosage;
        return this;
    }

    public String getPosologie() {
        return posologie;
    }

    public CustomerTraitementChronique setPosologie(String posologie) {
        this.posologie = posologie;
        return this;
    }

    public int getDureeJours() {
        return dureeJours;
    }

    public CustomerTraitementChronique setDureeJours(int dureeJours) {
        this.dureeJours = dureeJours;
        return this;
    }

    public LocalDate getDateOrdonnance() {
        return dateOrdonnance;
    }

    public CustomerTraitementChronique setDateOrdonnance(LocalDate dateOrdonnance) {
        this.dateOrdonnance = dateOrdonnance;
        return this;
    }

    public LocalDate getDateFinOrdonnance() {
        return dateFinOrdonnance;
    }

    public CustomerTraitementChronique setDateFinOrdonnance(LocalDate dateFinOrdonnance) {
        this.dateFinOrdonnance = dateFinOrdonnance;
        return this;
    }

    public String getNote() {
        return note;
    }

    public CustomerTraitementChronique setNote(String note) {
        this.note = note;
        return this;
    }

    public boolean isActif() {
        return actif;
    }

    public CustomerTraitementChronique setActif(boolean actif) {
        this.actif = actif;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public CustomerTraitementChronique setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public CustomerTraitementChronique setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public AppUser getUpdatedBy() {
        return updatedBy;
    }

    public CustomerTraitementChronique setUpdatedBy(AppUser updatedBy) {
        this.updatedBy = updatedBy;
        return this;
    }
}
