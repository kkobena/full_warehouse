package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * A SalesLine.
 */
@Entity
@Table(name = "sales_line",uniqueConstraints = {@UniqueConstraint(columnNames = {"produit_id","sales_id"})})
public class SalesLine implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "quantity_sold", nullable = false)
    private Integer quantitySold;

    @NotNull
    @Column(name = "regular_unit_price", nullable = false)
    private Integer regularUnitPrice;

    @NotNull
    @Column(name = "discount_unit_price", nullable = false)
    private Integer discountUnitPrice;

    @NotNull
    @Column(name = "net_unit_price", nullable = false)
    private Integer netUnitPrice;

    @NotNull
    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount;

    @NotNull
    @Column(name = "sales_amount", nullable = false)
    private Integer salesAmount;

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
    @Column(name = "cost_amount", nullable = false)
    private Integer costAmount;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "salesLines", allowSetters = true)
    private Sales sales;
    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "salesLines", allowSetters = true)
    private Produit produit;

 
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuantitySold() {
        return quantitySold;
    }

    public SalesLine quantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
        return this;
    }

    public void setQuantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
    }

    public Integer getRegularUnitPrice() {
        return regularUnitPrice;
    }

    public SalesLine regularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
        return this;
    }

    public void setRegularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
    }

    public Integer getDiscountUnitPrice() {
        return discountUnitPrice;
    }

    public SalesLine discountUnitPrice(Integer discountUnitPrice) {
        this.discountUnitPrice = discountUnitPrice;
        return this;
    }

    public void setDiscountUnitPrice(Integer discountUnitPrice) {
        this.discountUnitPrice = discountUnitPrice;
    }

    public Integer getNetUnitPrice() {
        return netUnitPrice;
    }

    public SalesLine netUnitPrice(Integer netUnitPrice) {
        this.netUnitPrice = netUnitPrice;
        return this;
    }

    public void setNetUnitPrice(Integer netUnitPrice) {
        this.netUnitPrice = netUnitPrice;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public SalesLine discountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
        return this;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getSalesAmount() {
        return salesAmount;
    }

    public SalesLine salesAmount(Integer salesAmount) {
        this.salesAmount = salesAmount;
        return this;
    }

    public void setSalesAmount(Integer salesAmount) {
        this.salesAmount = salesAmount;
    }

    public Integer getGrossAmount() {
        return grossAmount;
    }

    public SalesLine grossAmount(Integer grossAmount) {
        this.grossAmount = grossAmount;
        return this;
    }

    public void setGrossAmount(Integer grossAmount) {
        this.grossAmount = grossAmount;
    }

    public Integer getNetAmount() {
        return netAmount;
    }

    public SalesLine netAmount(Integer netAmount) {
        this.netAmount = netAmount;
        return this;
    }

    public void setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
    }

    public Integer getTaxAmount() {
        return taxAmount;
    }

    public SalesLine taxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
        return this;
    }

    public void setTaxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
    }

    public Integer getCostAmount() {
        return costAmount;
    }

    public SalesLine costAmount(Integer costAmount) {
        this.costAmount = costAmount;
        return this;
    }

    public void setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public SalesLine createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public SalesLine updatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Sales getSales() {
        return sales;
    }

    public SalesLine sales(Sales sales) {
        this.sales = sales;
        return this;
    }

    public void setSales(Sales sales) {
        this.sales = sales;
    }

    public Produit getProduit() {
        return produit;
    }

    public SalesLine produit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SalesLine)) {
            return false;
        }
        return id != null && id.equals(((SalesLine) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SalesLine{" +
            "id=" + getId() +
            ", quantitySold=" + getQuantitySold() +
            ", regularUnitPrice=" + getRegularUnitPrice() +
            ", discountUnitPrice=" + getDiscountUnitPrice() +
            ", netUnitPrice=" + getNetUnitPrice() +
            ", discountAmount=" + getDiscountAmount() +
            ", salesAmount=" + getSalesAmount() +
            ", grossAmount=" + getGrossAmount() +
            ", netAmount=" + getNetAmount() +
            ", taxAmount=" + getTaxAmount() +
            ", costAmount=" + getCostAmount() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
