package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A InventoryTransaction.
 */
@Entity
@Table(
    name = "inventory_transaction",
    indexes = {
        @Index(columnList = "transaction_type", name = "transaction_type_index"),
        @Index(columnList = "created_at", name = "createdAt_index"),
    }
)
public class InventoryTransaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
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

    @ManyToOne
    private FournisseurProduit fournisseurProduit;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotNull TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public @NotNull LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public @NotNull Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public @NotNull Integer getQuantityBefor() {
        return quantityBefor;
    }

    public void setQuantityBefor(Integer quantityBefor) {
        this.quantityBefor = quantityBefor;
    }

    public @NotNull Integer getQuantityAfter() {
        return quantityAfter;
    }

    public void setQuantityAfter(Integer quantityAfter) {
        this.quantityAfter = quantityAfter;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public @NotNull User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public @NotNull Integer getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
    }

    public @NotNull Integer getRegularUnitPrice() {
        return regularUnitPrice;
    }

    public void setRegularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
    }

    public @NotNull Magasin getMagasin() {
        return magasin;
    }

    public InventoryTransaction setMagasin(Magasin magasin) {
        this.magasin = magasin;
        return this;
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

    public Decondition getDecondition() {
        return decondition;
    }

    public InventoryTransaction setDecondition(Decondition decondition) {
        this.decondition = decondition;
        return this;
    }

    public FournisseurProduit getFournisseurProduit() {
        return fournisseurProduit;
    }

    public InventoryTransaction setFournisseurProduit(FournisseurProduit fournisseurProduit) {
        this.fournisseurProduit = fournisseurProduit;
        return this;
    }

    public InventoryTransaction transactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
        return this;
    }

    public InventoryTransaction createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public InventoryTransaction quantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public InventoryTransaction quantityBefor(Integer quantityBefor) {
        this.quantityBefor = quantityBefor;
        return this;
    }

    public InventoryTransaction quantityAfter(Integer quantityAfter) {
        this.quantityAfter = quantityAfter;
        return this;
    }

    public InventoryTransaction produit(Produit produit) {
        this.produit = produit;
        return this;
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
}
