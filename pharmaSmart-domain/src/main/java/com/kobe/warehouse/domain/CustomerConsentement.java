package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CanalConsentement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** Accord ou refus d'un client pour un canal de message ; la ligne la plus récente fait foi. */
@Entity
@Table(name = "customer_consentement")
public class CustomerConsentement implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 10)
    private CanalConsentement canal;

    @Column(name = "accorde", nullable = false)
    private boolean accorde;

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

    public CustomerConsentement setCustomerId(Integer customerId) {
        this.customerId = customerId;
        return this;
    }

    public CanalConsentement getCanal() {
        return canal;
    }

    public CustomerConsentement setCanal(CanalConsentement canal) {
        this.canal = canal;
        return this;
    }

    public boolean isAccorde() {
        return accorde;
    }

    public CustomerConsentement setAccorde(boolean accorde) {
        this.accorde = accorde;
        return this;
    }

    public AppUser getUser() {
        return user;
    }

    public CustomerConsentement setUser(AppUser user) {
        this.user = user;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public CustomerConsentement setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
