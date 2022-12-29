package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.SalesStatut;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

/**
 * A Payment.
 */
@Entity
@Table(name = "payment", indexes = {
    @Index(columnList = "ticket_code", name = "ticket_code_index")})
public class Payment implements Serializable, Cloneable {
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
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "payments", allowSetters = true)
    private PaymentMode paymentMode;
    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "payments", allowSetters = true)
    private DateDimension dateDimension;
    @NotNull
    @ManyToOne(optional = false)
    private User user;
    @ManyToOne
    @JsonIgnoreProperties(value = "payments", allowSetters = true)
    private Customer customer;
    @ManyToOne
    @JsonIgnoreProperties(value = "payments", allowSetters = true)
    private Sales sales;
    @NotNull
    @Column(name = "effective_update_date", nullable = false)
    private Instant effectiveUpdateDate;
    @NotNull
    @Column(name = "montant_verse", nullable = false)
    private Integer montantVerse = 0;
    @Size(max = 50)
    @Column(name = "ticket_code", length = 50)
    private String ticketCode;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "statut", nullable = false)
    private SalesStatut statut = SalesStatut.CLOSED;
    @Column(name = "part_assure", columnDefinition = "int default '0'")
    private Integer partAssure;
    @Column(name = "part_tiers_payant", columnDefinition = "int default '0'")
    private Integer partTiersPayant;
    @Column(name = "canceled", nullable = false, columnDefinition = "boolean default false")
    private Boolean canceled = false;

    public SalesStatut getStatut() {
        return statut;
    }

    public Payment setStatut(SalesStatut statut) {
        this.statut = statut;
        return this;
    }

    public Boolean getCanceled() {
        return canceled;
    }

    public Payment setCanceled(Boolean canceled) {
        this.canceled = canceled;
        return this;
    }

    public Integer getPartAssure() {
        return partAssure;
    }

    public Payment setPartAssure(Integer partAssure) {
        this.partAssure = partAssure;
        return this;
    }

    public Integer getPartTiersPayant() {
        return partTiersPayant;
    }

    public Payment setPartTiersPayant(Integer partTiersPayant) {
        this.partTiersPayant = partTiersPayant;
        return this;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public Payment setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
        return this;
    }

    public Integer getMontantVerse() {
        return montantVerse;
    }

    public Payment setMontantVerse(Integer montantVerse) {
        this.montantVerse = montantVerse;
        return this;
    }

    public Instant getEffectiveUpdateDate() {
        return effectiveUpdateDate;
    }

    public Payment setEffectiveUpdateDate(Instant effectiveUpdateDate) {
        this.effectiveUpdateDate = effectiveUpdateDate;
        return this;
    }

    public Sales getSales() {
        return sales;
    }

    public void setSales(Sales sales) {
        this.sales = sales;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

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

    public Payment netAmount(Integer netAmount) {
        this.netAmount = netAmount;
        return this;
    }

    public Integer getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Integer paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Payment paidAmount(Integer paidAmount) {
        this.paidAmount = paidAmount;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Payment createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Payment updatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    public Payment paymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
        return this;
    }

    public DateDimension getDateDimension() {
        return dateDimension;
    }

    public void setDateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
    }

    public Payment dateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
        return this;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Payment)) {
            return false;
        }
        return id != null && id.equals(((Payment) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Payment{" +
            "id=" + getId() +
            ", netAmount=" + getNetAmount() +
            ", paidAmount=" + getPaidAmount() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace(System.err);
            return null;

        }
    }
}
