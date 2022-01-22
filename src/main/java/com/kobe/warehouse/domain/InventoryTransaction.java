package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.time.Instant;

import com.kobe.warehouse.domain.enumeration.TransactionType;

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
	@Column(name = "amount", nullable = false)
	private Integer amount;
	@NotNull
	@Column(name = "created_at", nullable = false)
	private Instant createdAt=Instant.now();
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

	public TransactionType getTransactionType() {
		return transactionType;
	}

	public InventoryTransaction transactionType(TransactionType transactionType) {
		this.transactionType = transactionType;
		return this;
	}

	public void setTransactionType(TransactionType transactionType) {
		this.transactionType = transactionType;
	}

	public Integer getAmount() {
		return amount;
	}

	public InventoryTransaction amount(Integer amount) {
		this.amount = amount;
		return this;
	}

    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }

    public void setAmount(Integer amount) {
		this.amount = amount;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public InventoryTransaction createdAt(Instant createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public InventoryTransaction quantity(Integer quantity) {
		this.quantity = quantity;
		return this;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Integer getQuantityBefor() {
		return quantityBefor;
	}

	public InventoryTransaction quantityBefor(Integer quantityBefor) {
		this.quantityBefor = quantityBefor;
		return this;
	}

	public void setQuantityBefor(Integer quantityBefor) {
		this.quantityBefor = quantityBefor;
	}

	public Integer getQuantityAfter() {
		return quantityAfter;
	}

	public InventoryTransaction quantityAfter(Integer quantityAfter) {
		this.quantityAfter = quantityAfter;
		return this;
	}

	public void setQuantityAfter(Integer quantityAfter) {
		this.quantityAfter = quantityAfter;
	}

	public Produit getProduit() {
		return produit;
	}

	public InventoryTransaction produit(Produit produit) {
		this.produit = produit;
		return this;
	}

	public void setProduit(Produit produit) {
		this.produit = produit;
	}

	public DateDimension getDateDimension() {
		return dateDimension;
	}

	public InventoryTransaction dateDimension(DateDimension dateDimension) {
		this.dateDimension = dateDimension;
		return this;
	}

	public void setDateDimension(DateDimension dateDimension) {
		this.dateDimension = dateDimension;
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
}
