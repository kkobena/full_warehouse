package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.kobe.warehouse.domain.enumeration.OrderStatut;

/**
 * A Commande.
 */
@Entity
@Table(name = "commande")
public class Commande implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_refernce")
    private String orderRefernce;

    @NotNull
    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @NotNull
    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount;

    @NotNull
    @Column(name = "order_amount", nullable = false)
    private Integer orderAmount;

    @NotNull
    @Column(name = "gross_amount", nullable = false)
    private Integer grossAmount;

    @NotNull
    @Column(name = "net_amount", nullable = false)
    private Integer netAmount;

    @NotNull
    @Column(name = "tax_amount", nullable = false)
    private Integer taxAmount;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "order_status")
    private OrderStatut orderStatus;

    @OneToMany(mappedBy = "commande")
    private Set<PaymentFournisseur> paymentFournisseurs = new HashSet<>();

    @OneToMany(mappedBy = "commande",cascade = {CascadeType.PERSIST,CascadeType.REMOVE})
    private Set<OrderLine> orderLines = new HashSet<>();

    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "commandes", allowSetters = true)
    private DateDimension dateDimension;

    @ManyToOne(optional = false)
    private User user;
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

    public String getOrderRefernce() {
        return orderRefernce;
    }

    public Commande orderRefernce(String orderRefernce) {
        this.orderRefernce = orderRefernce;
        return this;
    }

    public void setOrderRefernce(String orderRefernce) {
        this.orderRefernce = orderRefernce;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public Commande receiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
        return this;
    }

    public void setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public Commande discountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
        return this;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getOrderAmount() {
        return orderAmount;
    }

    public Commande orderAmount(Integer orderAmount) {
        this.orderAmount = orderAmount;
        return this;
    }

    public void setOrderAmount(Integer orderAmount) {
        this.orderAmount = orderAmount;
    }

    public Integer getGrossAmount() {
        return grossAmount;
    }

    public Commande grossAmount(Integer grossAmount) {
        this.grossAmount = grossAmount;
        return this;
    }

    public void setGrossAmount(Integer grossAmount) {
        this.grossAmount = grossAmount;
    }

    public Integer getNetAmount() {
        return netAmount;
    }

    public Commande netAmount(Integer netAmount) {
        this.netAmount = netAmount;
        return this;
    }

    public void setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
    }

    public Integer getTaxAmount() {
        return taxAmount;
    }

    public Commande taxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
        return this;
    }

    public void setTaxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Commande createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Commande updatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OrderStatut getOrderStatus() {
        return orderStatus;
    }

    public Commande orderStatus(OrderStatut orderStatus) {
        this.orderStatus = orderStatus;
        return this;
    }

    public void setOrderStatus(OrderStatut orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Set<PaymentFournisseur> getPaymentFournisseurs() {
        return paymentFournisseurs;
    }

    public Commande paymentFournisseurs(Set<PaymentFournisseur> paymentFournisseurs) {
        this.paymentFournisseurs = paymentFournisseurs;
        return this;
    }

    public Commande addPaymentFournisseur(PaymentFournisseur paymentFournisseur) {
        this.paymentFournisseurs.add(paymentFournisseur);
        paymentFournisseur.setCommande(this);
        return this;
    }

    public Commande removePaymentFournisseur(PaymentFournisseur paymentFournisseur) {
        this.paymentFournisseurs.remove(paymentFournisseur);
        paymentFournisseur.setCommande(null);
        return this;
    }

    public void setPaymentFournisseurs(Set<PaymentFournisseur> paymentFournisseurs) {
        this.paymentFournisseurs = paymentFournisseurs;
    }

    public Set<OrderLine> getOrderLines() {
        return orderLines;
    }

    public Commande orderLines(Set<OrderLine> orderLines) {
        this.orderLines = orderLines;
        return this;
    }

    public Commande addOrderLine(OrderLine orderLine) {
        this.orderLines.add(orderLine);
        orderLine.setCommande(this);
        return this;
    }

    public Commande removeOrderLine(OrderLine orderLine) {
        this.orderLines.remove(orderLine);
        orderLine.setCommande(null);
        return this;
    }

    public void setOrderLines(Set<OrderLine> orderLines) {
        this.orderLines = orderLines;
    }

    public DateDimension getDateDimension() {
        return dateDimension;
    }

    public Commande dateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
        return this;
    }

    public void setDateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Commande)) {
            return false;
        }
        return id != null && id.equals(((Commande) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Commande{" +
            "id=" + getId() +
            ", orderRefernce='" + getOrderRefernce() + "'" +
            ", receiptDate='" + getReceiptDate() + "'" +
            ", discountAmount=" + getDiscountAmount() +
            ", orderAmount=" + getOrderAmount() +
            ", grossAmount=" + getGrossAmount() +
            ", netAmount=" + getNetAmount() +
            ", taxAmount=" + getTaxAmount() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", orderStatus='" + getOrderStatus() + "'" +
            "}";
    }
}
