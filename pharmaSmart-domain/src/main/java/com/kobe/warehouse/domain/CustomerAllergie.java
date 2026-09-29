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

/**
 * Allergie d'un client. Portée par une molécule ({@link Dci}), elle déclenche l'alerte à la vente ;
 * sur un libellé libre (arachide, latex…), elle n'est qu'affichée.
 */
@Entity
@Table(name = "customer_allergie")
public class CustomerAllergie implements Serializable {

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

    @Column(name = "libelle", length = 150)
    private String libelle;

    @Column(name = "reaction")
    private String reaction;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Integer getId() {
        return id;
    }

    public CustomerAllergie setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public CustomerAllergie setCustomerId(Integer customerId) {
        this.customerId = customerId;
        return this;
    }

    public Dci getDci() {
        return dci;
    }

    public CustomerAllergie setDci(Dci dci) {
        this.dci = dci;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public CustomerAllergie setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public String getReaction() {
        return reaction;
    }

    public CustomerAllergie setReaction(String reaction) {
        this.reaction = reaction;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public CustomerAllergie setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
