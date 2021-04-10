package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A OrderLine.
 */
@Entity
@Table(name = "order_line",uniqueConstraints ={@UniqueConstraint(columnNames = {"commande_id","produit_id"})})
public class OrderLine implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @NotNull
    @Column(name = "quantity_received", nullable = false)
    private Integer quantityReceived;

    @NotNull
    @Column(name = "quantity_requested", nullable = false)
    private Integer quantityRequested;

    @NotNull
    @Column(name = "quantity_returned", nullable = false)
    private Integer quantityReturned;

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
    @Column(name = "cost_amount", nullable = false)
    private Integer costAmount;

    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "orderLines", allowSetters = true)
    private Commande commande;

    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "orderLines", allowSetters = true)
    private Produit produit;
   
   
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public OrderLine receiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
        return this;
    }

    public void setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
    }

    public Integer getQuantityReceived() {
        return quantityReceived;
    }

    public OrderLine quantityReceived(Integer quantityReceived) {
        this.quantityReceived = quantityReceived;
        return this;
    }

    public void setQuantityReceived(Integer quantityReceived) {
        this.quantityReceived = quantityReceived;
    }

    public Integer getQuantityRequested() {
        return quantityRequested;
    }

    public OrderLine quantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
        return this;
    }

    public void setQuantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
    }

    public Integer getQuantityReturned() {
        return quantityReturned;
    }

    public OrderLine quantityReturned(Integer quantityReturned) {
        this.quantityReturned = quantityReturned;
        return this;
    }

    public void setQuantityReturned(Integer quantityReturned) {
        this.quantityReturned = quantityReturned;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public OrderLine discountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
        return this;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getOrderAmount() {
        return orderAmount;
    }

    public OrderLine orderAmount(Integer orderAmount) {
        this.orderAmount = orderAmount;
        return this;
    }

    public void setOrderAmount(Integer orderAmount) {
        this.orderAmount = orderAmount;
    }

    public Integer getGrossAmount() {
        return grossAmount;
    }

    public OrderLine grossAmount(Integer grossAmount) {
        this.grossAmount = grossAmount;
        return this;
    }

    public void setGrossAmount(Integer grossAmount) {
        this.grossAmount = grossAmount;
    }

    public Integer getNetAmount() {
        return netAmount;
    }

    public OrderLine netAmount(Integer netAmount) {
        this.netAmount = netAmount;
        return this;
    }

    public void setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
    }

    public Integer getTaxAmount() {
        return taxAmount;
    }

    public OrderLine taxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
        return this;
    }

    public void setTaxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public OrderLine createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public OrderLine updatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getCostAmount() {
        return costAmount;
    }

    public OrderLine costAmount(Integer costAmount) {
        this.costAmount = costAmount;
        return this;
    }

    public void setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
    }

    public Commande getCommande() {
        return commande;
    }

    public OrderLine commande(Commande commande) {
        this.commande = commande;
        return this;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }

    public Produit getProduit() {
        return produit;
    }

    public OrderLine produit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }
  

   

	@Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderLine)) {
            return false;
        }
        return id != null && id.equals(((OrderLine) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderLine{" +
            "id=" + getId() +
            ", receiptDate='" + getReceiptDate() + "'" +
            ", quantityReceived=" + getQuantityReceived() +
            ", quantityRequested=" + getQuantityRequested() +
            ", quantityReturned=" + getQuantityReturned() +
            ", discountAmount=" + getDiscountAmount() +
            ", orderAmount=" + getOrderAmount() +
            ", grossAmount=" + getGrossAmount() +
            ", netAmount=" + getNetAmount() +
            ", taxAmount=" + getTaxAmount() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", costAmount=" + getCostAmount() +
            "}";
    }
}
