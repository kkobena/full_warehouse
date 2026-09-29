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

/** Vente à crédit autorisée au-delà de la limite : pour quelle vente, par qui, pourquoi. */
@Entity
@Table(name = "limite_credit_derogation")
public class LimiteCreditDerogation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "sale_id", nullable = false)
    private Long saleId;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "montant", nullable = false)
    private int montant;

    @Column(name = "encours", nullable = false)
    private int encours;

    @Column(name = "limite", nullable = false)
    private int limite;

    @Column(name = "motif", nullable = false)
    private String motif;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autorise_par_id", nullable = false)
    private AppUser autorisePar;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Integer getId() {
        return id;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public LimiteCreditDerogation setCustomerId(Integer customerId) {
        this.customerId = customerId;
        return this;
    }

    public Long getSaleId() {
        return saleId;
    }

    public LimiteCreditDerogation setSaleId(Long saleId) {
        this.saleId = saleId;
        return this;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public LimiteCreditDerogation setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
        return this;
    }

    public int getMontant() {
        return montant;
    }

    public LimiteCreditDerogation setMontant(int montant) {
        this.montant = montant;
        return this;
    }

    public int getEncours() {
        return encours;
    }

    public LimiteCreditDerogation setEncours(int encours) {
        this.encours = encours;
        return this;
    }

    public int getLimite() {
        return limite;
    }

    public LimiteCreditDerogation setLimite(int limite) {
        this.limite = limite;
        return this;
    }

    public String getMotif() {
        return motif;
    }

    public LimiteCreditDerogation setMotif(String motif) {
        this.motif = motif;
        return this;
    }

    public AppUser getUser() {
        return user;
    }

    public LimiteCreditDerogation setUser(AppUser user) {
        this.user = user;
        return this;
    }

    public AppUser getAutorisePar() {
        return autorisePar;
    }

    public LimiteCreditDerogation setAutorisePar(AppUser autorisePar) {
        this.autorisePar = autorisePar;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
