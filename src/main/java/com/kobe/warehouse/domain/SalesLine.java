package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** A SalesLine. */
@Entity
@Table(
    name = "sales_line",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"produit_id", "sales_id"})})
public class SalesLine implements Serializable, Cloneable {

  @Serial private static final long serialVersionUID = 1L;

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
  private Integer quantityAvoir = 0;

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
  @Column(name = "ht_amount", nullable = false, columnDefinition = "int default '0'")
  private Integer htAmount = 0;

  @NotNull
  @Column(name = "tax_value", nullable = false, columnDefinition = "int default '0'")
  private Integer taxValue = 0;

  @NotNull
  @Column(name = "cost_amount", nullable = false, columnDefinition = "int default '0'")
  private Integer costAmount = 0;

  @NotNull
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @NotNull
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

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
  private LocalDateTime effectiveUpdateDate;

  @Column(name = "to_ignore", nullable = false)
  private boolean toIgnore = false;

  @Column(
      name = "amount_to_be_taken_into_account",
      nullable = false,
      columnDefinition = "int default '0'")
  private Integer amountToBeTakenIntoAccount = 0;

  @Column(name = "after_stock")
  private Integer afterStock;

  @Column(name = "init_stock")
  private Integer initStock;

  @Column(name = "tax_amount", nullable = false, columnDefinition = "int default '0'")
  private Integer taxAmount = 0;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public @NotNull Integer getQuantitySold() {
    return quantitySold;
  }

  public void setQuantitySold(Integer quantitySold) {
    this.quantitySold = quantitySold;
  }

  public @NotNull Integer getQuantityRequested() {
    return quantityRequested;
  }

  public SalesLine setQuantityRequested(Integer quantityRequested) {
    this.quantityRequested = quantityRequested;
    return this;
  }

  public @NotNull Integer getQuantityUg() {
    return quantityUg;
  }

  public SalesLine setQuantityUg(Integer quantityUg) {
    this.quantityUg = quantityUg;
    return this;
  }

  public @NotNull Integer getQuantityAvoir() {
    return quantityAvoir;
  }

  public SalesLine setQuantityAvoir(Integer quantiyAvoir) {
    quantityAvoir = quantiyAvoir;
    return this;
  }

  public @NotNull Integer getMontantTvaUg() {
    return montantTvaUg;
  }

  public SalesLine setMontantTvaUg(Integer montantTvaUg) {
    this.montantTvaUg = montantTvaUg;
    return this;
  }

  public @NotNull Integer getRegularUnitPrice() {
    return regularUnitPrice;
  }

  public void setRegularUnitPrice(Integer regularUnitPrice) {
    this.regularUnitPrice = regularUnitPrice;
  }

  public @NotNull Integer getDiscountUnitPrice() {
    return discountUnitPrice;
  }

  public void setDiscountUnitPrice(Integer discountUnitPrice) {
    this.discountUnitPrice = discountUnitPrice;
  }

  public @NotNull Integer getNetUnitPrice() {
    return netUnitPrice;
  }

  public void setNetUnitPrice(Integer netUnitPrice) {
    this.netUnitPrice = netUnitPrice;
  }

  public @NotNull Integer getDiscountAmount() {
    return discountAmount;
  }

  public void setDiscountAmount(Integer discountAmount) {
    this.discountAmount = discountAmount;
  }

  public @NotNull Integer getDiscountAmountHorsUg() {
    return discountAmountHorsUg;
  }

  public SalesLine setDiscountAmountHorsUg(Integer discountAmountHorsUg) {
    this.discountAmountHorsUg = discountAmountHorsUg;
    return this;
  }

  public @NotNull Integer getDiscountAmountUg() {
    return discountAmountUg;
  }

  public SalesLine setDiscountAmountUg(Integer discountAmountUg) {
    this.discountAmountUg = discountAmountUg;
    return this;
  }

  public @NotNull Integer getSalesAmount() {
    return salesAmount;
  }

  public void setSalesAmount(Integer salesAmount) {
    this.salesAmount = salesAmount;
  }

  public @NotNull Integer getNetAmount() {
    return netAmount;
  }

  public void setNetAmount(Integer netAmount) {
    this.netAmount = netAmount;
  }

  public @NotNull Integer getHtAmount() {
    return htAmount;
  }

  public void setHtAmount(@NotNull Integer htAmount) {
    this.htAmount = htAmount;
  }

  public @NotNull Integer getTaxValue() {
    return taxValue;
  }

  public SalesLine setTaxValue(Integer taxValue) {
    this.taxValue = taxValue;
    return this;
  }

  public @NotNull Integer getCostAmount() {
    return costAmount;
  }

  public void setCostAmount(Integer costAmount) {
    this.costAmount = costAmount;
  }

  public @NotNull LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public @NotNull LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public @NotNull Sales getSales() {
    return sales;
  }

  public void setSales(Sales sales) {
    this.sales = sales;
  }

  public @NotNull Produit getProduit() {
    return produit;
  }

  public void setProduit(Produit produit) {
    this.produit = produit;
  }

  public @NotNull LocalDateTime getEffectiveUpdateDate() {
    return effectiveUpdateDate;
  }

  public SalesLine setEffectiveUpdateDate(LocalDateTime effectiveUpdateDate) {
    this.effectiveUpdateDate = effectiveUpdateDate;
    return this;
  }

  public boolean isToIgnore() {
    return toIgnore;
  }

  public SalesLine setToIgnore(boolean toIgnore) {
    this.toIgnore = toIgnore;
    return this;
  }

  public Integer getAmountToBeTakenIntoAccount() {
    return amountToBeTakenIntoAccount;
  }

  public SalesLine setAmountToBeTakenIntoAccount(Integer amountToBeTakenIntoAccount) {
    this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
    return this;
  }

  public Integer getAfterStock() {
    return afterStock;
  }

  public SalesLine setAfterStock(Integer afterStock) {
    this.afterStock = afterStock;
    return this;
  }

  public Integer getInitStock() {
    return initStock;
  }

  public SalesLine setInitStock(Integer initStock) {
    this.initStock = initStock;
    return this;
  }

  public Integer getTaxAmount() {
    return taxAmount;
  }

  public void setTaxAmount(Integer taxAmount) {
    this.taxAmount = taxAmount;
  }

  public SalesLine quantitySold(Integer quantitySold) {
    this.quantitySold = quantitySold;
    return this;
  }

  public SalesLine regularUnitPrice(Integer regularUnitPrice) {
    this.regularUnitPrice = regularUnitPrice;
    return this;
  }

  public SalesLine discountUnitPrice(Integer discountUnitPrice) {
    this.discountUnitPrice = discountUnitPrice;
    return this;
  }

  public SalesLine netUnitPrice(Integer netUnitPrice) {
    this.netUnitPrice = netUnitPrice;
    return this;
  }

  public SalesLine discountAmount(Integer discountAmount) {
    this.discountAmount = discountAmount;
    return this;
  }

  public SalesLine salesAmount(Integer salesAmount) {
    this.salesAmount = salesAmount;
    return this;
  }

  public SalesLine netAmount(Integer netAmount) {
    this.netAmount = netAmount;
    return this;
  }

  public SalesLine costAmount(Integer costAmount) {
    this.costAmount = costAmount;
    return this;
  }

  public SalesLine createdAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public SalesLine updatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public SalesLine sales(Sales sales) {
    this.sales = sales;
    return this;
  }

  public SalesLine produit(Produit produit) {
    this.produit = produit;
    return this;
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
    return "SalesLine{"
        + "id="
        + getId()
        + ", quantitySold="
        + getQuantitySold()
        + ", regularUnitPrice="
        + getRegularUnitPrice()
        + ", discountUnitPrice="
        + getDiscountUnitPrice()
        + ", netUnitPrice="
        + getNetUnitPrice()
        + ", discountAmount="
        + getDiscountAmount()
        + ", salesAmount="
        + getSalesAmount()
        + ", netAmount="
        + getNetAmount()
        + ", costAmount="
        + getCostAmount()
        + ", createdAt='"
        + getCreatedAt()
        + "'"
        + ", updatedAt='"
        + getUpdatedAt()
        + "'"
        + "}";
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
