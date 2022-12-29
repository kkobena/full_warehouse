package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.TransactionType;

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
import java.io.Serializable;
import java.time.Instant;

/**
 * A InventoryTransaction.
 */
@Entity
@Table(name = "inventory_transaction", indexes = {
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
    private Instant createdAt = Instant.now();
    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    @NotNull
    @Column(name = "quantity_befor", nullable = false)
    private Integer quantityBefor;
    @NotNull
    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;
    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = "inventoryTransactions", allowSetters = true)
    private Produit produit;
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "inventoryTransactions", allowSetters = true)
    private DateDimension dateDimension;
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
    @ManyToOne
    private Ajustement ajustement;
    @ManyToOne
    private SalesLine saleLine;
    @ManyToOne
    private OrderLine orderLine;
    @ManyToOne
    private RepartitionStockProduit repartitionStockProduit;
    @ManyToOne
    private Decondition decondition;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Decondition getDecondition() {
        return decondition;
    }

    public InventoryTransaction setDecondition(Decondition decondition) {
        this.decondition = decondition;
        return this;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public InventoryTransaction transactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
        return this;
    }

    public Magasin getMagasin() {
        return magasin;
    }

    public InventoryTransaction setMagasin(Magasin magasin) {
        this.magasin = magasin;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public InventoryTransaction createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public InventoryTransaction quantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public Integer getQuantityBefor() {
        return quantityBefor;
    }

    public void setQuantityBefor(Integer quantityBefor) {
        this.quantityBefor = quantityBefor;
    }

    public InventoryTransaction quantityBefor(Integer quantityBefor) {
        this.quantityBefor = quantityBefor;
        return this;
    }

    public Integer getQuantityAfter() {
        return quantityAfter;
    }

    public void setQuantityAfter(Integer quantityAfter) {
        this.quantityAfter = quantityAfter;
    }

    public InventoryTransaction quantityAfter(Integer quantityAfter) {
        this.quantityAfter = quantityAfter;
        return this;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public InventoryTransaction produit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public DateDimension getDateDimension() {
        return dateDimension;
    }

    public void setDateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
    }

    public InventoryTransaction dateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
        return this;
    }

    public Integer getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
    }

    public Integer getRegularUnitPrice() {
        return regularUnitPrice;
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

    public Ajustement getAjustement() {
        return ajustement;
    }

    public InventoryTransaction setAjustement(Ajustement ajustement) {
        this.ajustement = ajustement;
        return this;
    }

    public SalesLine getSaleLine() {
        return saleLine;
    }

    public InventoryTransaction setSaleLine(SalesLine saleLine) {
        this.saleLine = saleLine;
        return this;
    }

    public OrderLine getOrderLine() {
        return orderLine;
    }

    public InventoryTransaction setOrderLine(OrderLine orderLine) {
        this.orderLine = orderLine;
        return this;
    }

    public RepartitionStockProduit getRepartitionStockProduit() {
        return repartitionStockProduit;
    }

    public InventoryTransaction setRepartitionStockProduit(RepartitionStockProduit repartitionStockProduit) {
        this.repartitionStockProduit = repartitionStockProduit;
        return this;
    }
}
