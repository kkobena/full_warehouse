package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

/**
 * A PaymentFournisseur.
 */
@Entity
@Table(name = "payment_fournisseur")
public class PaymentFournisseur implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "net_amount", nullable = false)
    private Integer netAmount;

    @NotNull
    @Column(name = "paid_amount", nullable = false)
    private Integer paidAmount;

    @NotNull
    @Column(name = "rest_to_pay", nullable = false)
    private Integer restToPay;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne
    @JsonIgnoreProperties(value = "paymentFournisseurs", allowSetters = true)
    private Commande commande;

    @ManyToOne
    @JsonIgnoreProperties(value = "paymentFournisseurs", allowSetters = true)
    private PaymentMode paymentMode;

    @ManyToOne
    @JsonIgnoreProperties(value = "paymentFournisseurs", allowSetters = true)
    private DateDimension dateDimension;

    // jhipster-needle-entity-add-field - JHipster will add fields here
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
    }

    public PaymentFournisseur netAmount(Integer netAmount) {
        this.netAmount = netAmount;
        return this;
    }

    public Integer getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Integer paidAmount) {
        this.paidAmount = paidAmount;
    }

    public PaymentFournisseur paidAmount(Integer paidAmount) {
        this.paidAmount = paidAmount;
        return this;
    }

    public Integer getRestToPay() {
        return restToPay;
    }

    public void setRestToPay(Integer restToPay) {
        this.restToPay = restToPay;
    }

    public PaymentFournisseur restToPay(Integer restToPay) {
        this.restToPay = restToPay;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public PaymentFournisseur createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public PaymentFournisseur updatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Commande getCommande() {
        return commande;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }

    public PaymentFournisseur commande(Commande commande) {
        this.commande = commande;
        return this;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    public PaymentFournisseur paymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
        return this;
    }

    public DateDimension getDateDimension() {
        return dateDimension;
    }

    public void setDateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
    }

    public PaymentFournisseur dateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
        return this;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaymentFournisseur)) {
            return false;
        }
        return id != null && id.equals(((PaymentFournisseur) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PaymentFournisseur{" +
            "id=" + getId() +
            ", netAmount=" + getNetAmount() +
            ", paidAmount=" + getPaidAmount() +
            ", restToPay=" + getRestToPay() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
