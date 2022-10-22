package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A SalesLine.
 */
@Entity
@Table(name = "sales_line", uniqueConstraints = {@UniqueConstraint(columnNames = {"produit_id", "sales_id"})})
public class SalesLine implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Column(name = "quantity_sold", nullable = false)
    private Integer quantitySold;
    @NotNull
    @Column(name = "quantity_requested", nullable = false)
    private Integer quantityRequested;
    @NotNull
    @Column(name = "quantity_ug", nullable = false, columnDefinition = "int default '0'")
    private Integer quantityUg = 0;
    @NotNull
    @Column(name = "quantity_avoir", nullable = false, columnDefinition = "int default '0'")
    private Integer quantiyAvoir = 0;
    @NotNull
    @Column(name = "montant_tva_ug", nullable = false, columnDefinition = "int default '0'")
    private Integer montantTvaUg = 0;
    @NotNull
    @Column(name = "regular_unit_price", nullable = false, columnDefinition = "int default '0'")
    private Integer regularUnitPrice;
    @NotNull
    @Column(name = "discount_unit_price", nullable = false, columnDefinition = "int default '0'")
    private Integer discountUnitPrice = 0;
    @NotNull
    @Column(name = "net_unit_price", nullable = false, columnDefinition = "int default '0'")
    private Integer netUnitPrice = 0;
    @NotNull
    @Column(name = "discount_amount", nullable = false, columnDefinition = "int default '0'")
    private Integer discountAmount = 0;
    @NotNull
    @Column(name = "discount_amount_hors_ug", nullable = false, columnDefinition = "int default '0'")
    private Integer discountAmountHorsUg = 0;
    @NotNull
    @Column(name = "discount_amount_ug", nullable = false, columnDefinition = "int default '0'")
    private Integer discountAmountUg = 0;
    @NotNull
    @Column(name = "sales_amount", nullable = false, columnDefinition = "int default '0'")
    private Integer salesAmount = 0;
    @NotNull
    @Column(name = "net_amount", nullable = false, columnDefinition = "int default '0'")
    private Integer netAmount = 0;
    @NotNull
    @Column(name = "tax_value", nullable = false, columnDefinition = "int default '0'")
    private Integer taxValue = 0;
    @NotNull
    @Column(name = "cost_amount", nullable = false, columnDefinition = "int default '0'")
    private Integer costAmount = 0;
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
    @NotNull
    @Column(name = "effective_update_date", nullable = false)
    private Instant effectiveUpdateDate;
    @Column(name = "to_ignore", nullable = false)
    private boolean toIgnore = false;
    @Column(name = "amount_to_be_taken_into_account", nullable = false, columnDefinition = "int default '0'")
    private Integer amountToBeTakenIntoAccount = 0;

    public Integer getDiscountAmountHorsUg() {
        return discountAmountHorsUg;
    }

    public SalesLine setDiscountAmountHorsUg(Integer discountAmountHorsUg) {
        this.discountAmountHorsUg = discountAmountHorsUg;
        return this;
    }

    public Integer getDiscountAmountUg() {
        return discountAmountUg;
    }

    public SalesLine setDiscountAmountUg(Integer discountAmountUg) {
        this.discountAmountUg = discountAmountUg;
        return this;
    }

    public Integer getTaxValue() {
        return taxValue;
    }

    public SalesLine setTaxValue(Integer taxValue) {
        this.taxValue = taxValue;
        return this;
    }

    public Integer getQuantityUg() {
        return quantityUg;
    }

    public SalesLine setQuantityUg(Integer quantityUg) {
        this.quantityUg = quantityUg;
        return this;
    }

    public Integer getQuantityRequested() {
        return quantityRequested;
    }

    public SalesLine setQuantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
        return this;
    }

    public Integer getQuantiyAvoir() {
        return quantiyAvoir;
    }

    public SalesLine setQuantiyAvoir(Integer quantiyAvoir) {
        this.quantiyAvoir = quantiyAvoir;
        return this;
    }

    public Integer getMontantTvaUg() {
        return montantTvaUg;
    }

    public SalesLine setMontantTvaUg(Integer montantTvaUg) {
        this.montantTvaUg = montantTvaUg;
        return this;
    }

    public Integer getAmountToBeTakenIntoAccount() {
        return amountToBeTakenIntoAccount;
    }

    public SalesLine setAmountToBeTakenIntoAccount(Integer amountToBeTakenIntoAccount) {
        this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
        return this;
    }

    public boolean isToIgnore() {
        return toIgnore;
    }

    public SalesLine setToIgnore(boolean toIgnore) {
        this.toIgnore = toIgnore;
        return this;
    }

    public Instant getEffectiveUpdateDate() {
        return effectiveUpdateDate;
    }

    public SalesLine setEffectiveUpdateDate(Instant effectiveUpdateDate) {
        this.effectiveUpdateDate = effectiveUpdateDate;
        return this;
    }

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
        return Objects.hash(id);
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

            ", netAmount=" + getNetAmount() +
            ", costAmount=" + getCostAmount() +
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
