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
import java.time.LocalDateTime;

/** Délivrance faite malgré une alerte santé : qui a délivré, qui a autorisé, pourquoi. */
@Entity
@Table(name = "alerte_sante_derogation")
public class AlerteSanteDerogation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    /** Les alertes présentées, en clair : ce que l'utilisateur a vu au moment de passer outre. */
    @Column(name = "alertes", nullable = false, columnDefinition = "text")
    private String alertes;

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

    public AlerteSanteDerogation setCustomerId(Integer customerId) {
        this.customerId = customerId;
        return this;
    }

    public Produit getProduit() {
        return produit;
    }

    public AlerteSanteDerogation setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public String getAlertes() {
        return alertes;
    }

    public AlerteSanteDerogation setAlertes(String alertes) {
        this.alertes = alertes;
        return this;
    }

    public String getMotif() {
        return motif;
    }

    public AlerteSanteDerogation setMotif(String motif) {
        this.motif = motif;
        return this;
    }

    public AppUser getUser() {
        return user;
    }

    public AlerteSanteDerogation setUser(AppUser user) {
        this.user = user;
        return this;
    }

    public AppUser getAutorisePar() {
        return autorisePar;
    }

    public AlerteSanteDerogation setAutorisePar(AppUser autorisePar) {
        this.autorisePar = autorisePar;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
