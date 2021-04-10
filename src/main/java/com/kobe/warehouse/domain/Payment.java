package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * A Payment.
 */
@Entity
@Table(name = "payment")
public class Payment implements Serializable {

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
    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "payments", allowSetters = true)
    private Customer customer;
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "payments", allowSetters = true)
    private Sales sales;
    
    
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

    public Payment netAmount(Integer netAmount) {
        this.netAmount = netAmount;
        return this;
    }

    public void setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
    }

    public Integer getPaidAmount() {
        return paidAmount;
    }

    public Payment paidAmount(Integer paidAmount) {
        this.paidAmount = paidAmount;
        return this;
    }

    public void setPaidAmount(Integer paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Integer getRestToPay() {
        return restToPay;
    }

    public Payment restToPay(Integer restToPay) {
        this.restToPay = restToPay;
        return this;
    }

    public void setRestToPay(Integer restToPay) {
        this.restToPay = restToPay;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Payment createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Payment updatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public Payment paymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
        return this;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    public DateDimension getDateDimension() {
        return dateDimension;
    }

    public Payment dateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
        return this;
    }

    public void setDateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
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
            ", restToPay=" + getRestToPay() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
