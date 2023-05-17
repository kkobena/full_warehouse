package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.Formula;

@Entity
@Table(
    name = "delivery_receipt_item",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"delivery_receipt_id", "fournisseur_produit_id"})
    })
public class DeliveryReceiptItem implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "quantity_received", nullable = false)
  private Integer quantityReceived;

  @NotNull
  @Column(name = "init_stock", nullable = false)
  private Integer initStock;

  @NotNull
  @Column(name = "quantity_requested", nullable = false)
  private Integer quantityRequested;

  @Column(name = "quantity_returned")
  private Integer quantityReturned;

  @Column(name = "discount_amount", nullable = false, columnDefinition = "int default '0'")
  private Integer discountAmount = 0;

  @Column(name = "net_amount", columnDefinition = "int default '0'")
  private Integer netAmount = 0;

  @Column(name = "tax_amount", columnDefinition = "int default '0'")
  private Integer taxAmount = 0;

  @NotNull
  @Column(name = "created_date", nullable = false)
  private LocalDateTime createdDate;
  @Column(name = "updated_date")
  private LocalDateTime updatedDate = LocalDateTime.now();

  @ManyToOne(optional = false)
  @JsonIgnoreProperties(value = "receiptItems", allowSetters = true)
  private DeliveryReceipt deliveryReceipt;

  @NotNull
  @Column(name = "order_unit_price", nullable = false)
  private Integer orderUnitPrice; // prix uni commande

  @NotNull
  @Column(name = "regular_unit_price", nullable = false, columnDefinition = "int default '0'")
  private Integer regularUnitPrice; // prix unitaire machine

  @NotNull
  @Column(name = "order_cost_amount", nullable = false, columnDefinition = "int default '0'")
  private Integer orderCostAmount; // prix d'achat commande

  @Formula("quantity_requested*order_cost_amount")
  private Integer effectifGrossIncome; // montant achat commande

  @Formula("quantity_requested*order_unit_price")
  private Integer effectifOrderAmount; // montant vente commande

  @Column(name = "quantity_ug", columnDefinition = "int default '0'")
  private Integer ugQuantity = 0;

  @ManyToOne(optional = false)
  private FournisseurProduit fournisseurProduit;

  @OneToMany(mappedBy = "receiptItem")
  private List<Lot> lots = new ArrayList<>();

  @Column(name = "is_updated")
  private Boolean updated = Boolean.FALSE;

  @Column(name = "cost_amount")
  private Integer costAmount;

  public Long getId() {
    return id;
  }

  public DeliveryReceiptItem setId(Long id) {
    this.id = id;
    return this;
  }

  public LocalDateTime getUpdatedDate() {
    return updatedDate;
  }

  public DeliveryReceiptItem setUpdatedDate(LocalDateTime updatedDate) {
    this.updatedDate = updatedDate;
    return this;
  }

  public Integer getCostAmount() {
    return costAmount;
  }

  public DeliveryReceiptItem setCostAmount(Integer costAmount) {
    this.costAmount = costAmount;
    return this;
  }

  public Integer getQuantityReceived() {
    return quantityReceived;
  }

  public DeliveryReceiptItem setQuantityReceived(Integer quantityReceived) {
    this.quantityReceived = quantityReceived;
    return this;
  }

  public Integer getInitStock() {
    return initStock;
  }

  public DeliveryReceiptItem setInitStock(Integer initStock) {
    this.initStock = initStock;
    return this;
  }

  public Integer getQuantityRequested() {
    return quantityRequested;
  }

  public DeliveryReceiptItem setQuantityRequested(Integer quantityRequested) {
    this.quantityRequested = quantityRequested;
    return this;
  }

  public Integer getQuantityReturned() {
    return quantityReturned;
  }

  public DeliveryReceiptItem setQuantityReturned(Integer quantityReturned) {
    this.quantityReturned = quantityReturned;
    return this;
  }

  public Integer getDiscountAmount() {
    return discountAmount;
  }

  public DeliveryReceiptItem setDiscountAmount(Integer discountAmount) {
    this.discountAmount = discountAmount;
    return this;
  }

  public Boolean getUpdated() {
    return updated;
  }

  public DeliveryReceiptItem setUpdated(Boolean updated) {
    this.updated = updated;
    return this;
  }

  public Integer getNetAmount() {
    return netAmount;
  }

  public DeliveryReceiptItem setNetAmount(Integer netAmount) {
    this.netAmount = netAmount;
    return this;
  }

  public Integer getTaxAmount() {
    return taxAmount;
  }

  public DeliveryReceiptItem setTaxAmount(Integer taxAmount) {
    this.taxAmount = taxAmount;
    return this;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public DeliveryReceiptItem setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  public DeliveryReceipt getDeliveryReceipt() {
    return deliveryReceipt;
  }

  public DeliveryReceiptItem setDeliveryReceipt(DeliveryReceipt deliveryReceipt) {
    this.deliveryReceipt = deliveryReceipt;
    return this;
  }

  public Integer getOrderUnitPrice() {
    return orderUnitPrice;
  }

  public DeliveryReceiptItem setOrderUnitPrice(Integer orderUnitPrice) {
    this.orderUnitPrice = orderUnitPrice;
    return this;
  }

  public Integer getRegularUnitPrice() {
    return regularUnitPrice;
  }

  public DeliveryReceiptItem setRegularUnitPrice(Integer regularUnitPrice) {
    this.regularUnitPrice = regularUnitPrice;
    return this;
  }

  public Integer getOrderCostAmount() {
    return orderCostAmount;
  }

  public DeliveryReceiptItem setOrderCostAmount(Integer orderCostAmount) {
    this.orderCostAmount = orderCostAmount;
    return this;
  }

  public Integer getEffectifGrossIncome() {
    return effectifGrossIncome;
  }

  public DeliveryReceiptItem setEffectifGrossIncome(Integer effectifGrossIncome) {
    this.effectifGrossIncome = effectifGrossIncome;
    return this;
  }

  public Integer getEffectifOrderAmount() {
    return effectifOrderAmount;
  }

  public DeliveryReceiptItem setEffectifOrderAmount(Integer effectifOrderAmount) {
    this.effectifOrderAmount = effectifOrderAmount;
    return this;
  }

  public Integer getUgQuantity() {
    return ugQuantity;
  }

  public DeliveryReceiptItem setUgQuantity(Integer ugQuantity) {
    this.ugQuantity = ugQuantity;
    return this;
  }

  public FournisseurProduit getFournisseurProduit() {
    return fournisseurProduit;
  }

  public DeliveryReceiptItem setFournisseurProduit(FournisseurProduit fournisseurProduit) {
    this.fournisseurProduit = fournisseurProduit;
    return this;
  }

  public List<Lot> getLots() {
    return lots;
  }

  public DeliveryReceiptItem setLots(List<Lot> lots) {
    this.lots = lots;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DeliveryReceiptItem that = (DeliveryReceiptItem) o;
    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
