package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * A PaymentFournisseur.
 */
@Getter
@Entity
@Table(name = "payment_fournisseur")
public class PaymentFournisseur implements Serializable {

    private static final long serialVersionUID = 1L;

  // jhipster-needle-entity-add-field - JHipster will add fields here
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
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne
    @JsonIgnoreProperties(value = "paymentFournisseurs", allowSetters = true)
    private DeliveryReceipt deliveryReceipt;

    @ManyToOne
    @JsonIgnoreProperties(value = "paymentFournisseurs", allowSetters = true)
    private PaymentMode paymentMode;
    @ManyToOne(optional = false)
    @NotNull
    private WarehouseCalendar calendar;

    public PaymentFournisseur setCalendar(WarehouseCalendar calendar) {
        this.calendar = calendar;
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

  public void setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
    }

    public PaymentFournisseur netAmount(Integer netAmount) {
        this.netAmount = netAmount;
        return this;
    }

  public void setPaidAmount(Integer paidAmount) {
        this.paidAmount = paidAmount;
    }

    public PaymentFournisseur paidAmount(Integer paidAmount) {
        this.paidAmount = paidAmount;
        return this;
    }

  public void setRestToPay(Integer restToPay) {
        this.restToPay = restToPay;
    }

    public PaymentFournisseur restToPay(Integer restToPay) {
        this.restToPay = restToPay;
        return this;
    }

  public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public PaymentFournisseur createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

  public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public PaymentFournisseur updatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

  public PaymentFournisseur setDeliveryReceipt(DeliveryReceipt deliveryReceipt) {
        this.deliveryReceipt = deliveryReceipt;
        return this;
    }

  public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
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
