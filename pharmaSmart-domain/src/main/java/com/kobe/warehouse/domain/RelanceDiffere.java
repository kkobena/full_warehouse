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

/** Relance SMS d'un client pour ses ventes différées : ce qui a été envoyé, à qui, par qui. */
@Entity
@Table(name = "relance_differe")
public class RelanceDiffere implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "telephone", nullable = false, length = 30)
    private String telephone;

    @Column(name = "montant", nullable = false)
    private int montant;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Integer getId() {
        return id;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public RelanceDiffere setCustomerId(Integer customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getTelephone() {
        return telephone;
    }

    public RelanceDiffere setTelephone(String telephone) {
        this.telephone = telephone;
        return this;
    }

    public int getMontant() {
        return montant;
    }

    public RelanceDiffere setMontant(int montant) {
        this.montant = montant;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public RelanceDiffere setMessage(String message) {
        this.message = message;
        return this;
    }

    public AppUser getUser() {
        return user;
    }

    public RelanceDiffere setUser(AppUser user) {
        this.user = user;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
