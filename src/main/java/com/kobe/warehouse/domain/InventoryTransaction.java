package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import java.io.Serializable;
import java.time.LocalDateTime;
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
import lombok.Getter;

/** A InventoryTransaction. */
@Getter
@Entity
@Table(
    name = "inventory_transaction",
    indexes = {
      @Index(columnList = "transaction_type", name = "transaction_type_index"),
      @Index(columnList = "created_at", name = "createdAt_index")
    })
public class InventoryTransaction implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "transaction_type", nullable = false)
  private TransactionType transactionType;

  @NotNull
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @NotNull
  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @NotNull
  @Column(name = "quantity_befor", nullable = false)
  private Integer quantityBefor;

  @NotNull
  @Column(name = "quantity_after", nullable = false)
  private Integer quantityAfter;

  @ManyToOne
  @JsonIgnoreProperties(value = "inventoryTransactions", allowSetters = true)
  private Produit produit;

  @ManyToOne(optional = false)
  @NotNull
  private User user;

  @NotNull
  @Column(name = "cost_amount", nullable = false)
  private Integer costAmount;

  @NotNull
  @Column(name = "regular_unit_price", nullable = false)
  private Integer regularUnitPrice;

  @ManyToOne(optional = false)
  @NotNull
  private Magasin magasin;

  @ManyToOne private Ajustement ajustement;
  @ManyToOne private SalesLine saleLine;
  @ManyToOne private DeliveryReceiptItem deliveryReceiptItem;
  @ManyToOne private RepartitionStockProduit repartitionStockProduit;
  @ManyToOne private Decondition decondition;
  @ManyToOne private FournisseurProduit fournisseurProduit;

  public void setUser(User user) {
    this.user = user;
  }

  public InventoryTransaction setFournisseurProduit(FournisseurProduit fournisseurProduit) {
    this.fournisseurProduit = fournisseurProduit;
    return this;
  }

  public InventoryTransaction setDecondition(Decondition decondition) {
    this.decondition = decondition;
    return this;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setTransactionType(TransactionType transactionType) {
    this.transactionType = transactionType;
  }

  public InventoryTransaction transactionType(TransactionType transactionType) {
    this.transactionType = transactionType;
    return this;
  }

  public InventoryTransaction setMagasin(Magasin magasin) {
    this.magasin = magasin;
    return this;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public InventoryTransaction createdAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public InventoryTransaction quantity(Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  public void setQuantityBefor(Integer quantityBefor) {
    this.quantityBefor = quantityBefor;
  }

  public InventoryTransaction quantityBefor(Integer quantityBefor) {
    this.quantityBefor = quantityBefor;
    return this;
  }

  public void setQuantityAfter(Integer quantityAfter) {
    this.quantityAfter = quantityAfter;
  }

  public InventoryTransaction quantityAfter(Integer quantityAfter) {
    this.quantityAfter = quantityAfter;
    return this;
  }

  public void setProduit(Produit produit) {
    this.produit = produit;
  }

  public InventoryTransaction produit(Produit produit) {
    this.produit = produit;
    return this;
  }

  public void setCostAmount(Integer costAmount) {
    this.costAmount = costAmount;
  }

  public void setRegularUnitPrice(Integer regularUnitPrice) {
    this.regularUnitPrice = regularUnitPrice;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof InventoryTransaction)) {
      return false;
    }
    return id != null && id.equals(((InventoryTransaction) o).id);
  }

  @Override
  public int hashCode() {
    return 31;
  }

  public InventoryTransaction setAjustement(Ajustement ajustement) {
    this.ajustement = ajustement;
    return this;
  }

  public InventoryTransaction setSaleLine(SalesLine saleLine) {
    this.saleLine = saleLine;
    return this;
  }

  public InventoryTransaction setRepartitionStockProduit(
      RepartitionStockProduit repartitionStockProduit) {
    this.repartitionStockProduit = repartitionStockProduit;
    return this;
  }

  public InventoryTransaction setDeliveryReceiptItem(DeliveryReceiptItem deliveryReceiptItem) {
    this.deliveryReceiptItem = deliveryReceiptItem;
    return this;
  }
}
