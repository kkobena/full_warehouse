package com.kobe.warehouse.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.TypeProduit;

import io.swagger.annotations.ApiModel;

/**
 * not an ignored comment
 */
@ApiModel(description = "not an ignored comment")
@Entity
@Table(name = "produit", uniqueConstraints = { @UniqueConstraint(columnNames = { "libelle", "type_produit" }) })
public class Produit implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@Column(name = "libelle", nullable = false)
	private String libelle;

	@Column(name = "code", nullable = false)
	private String code;

	@Column(name = "image_url")
	private String imageUrl;
    @Lob
    @Column(name = "data")
    private String data;
    @Column(name = "image_type")
    private String imageType;
	@Enumerated(EnumType.STRING)
	@Column(name = "type_produit", nullable = false)
	private TypeProduit typeProduit;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@NotNull
	@Column(name = "cost_amount", nullable = false)
	private Integer costAmount;

	@NotNull
	@Column(name = "regular_unit_price", nullable = false)
	private Integer regularUnitPrice;

	@Column(name = "net_unit_price", nullable = false)
	private Integer netUnitPrice;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@NotNull
	@Min(value = 0)
	@Column(name = "item_qty", nullable = false)
	private Integer itemQty;

	@NotNull
	@Min(value = 0)
	@Column(name = "item_cost_amount", nullable = false)
	private Integer itemCostAmount;

	@NotNull
	@Min(value = 0)
	@Column(name = "item_regular_unit_price", nullable = false)
	private Integer itemRegularUnitPrice;

	@OneToMany(mappedBy = "produit")
	private Set<SalesLine> salesLines = new HashSet<>();

	@OneToMany(mappedBy = "produit")
	private Set<StoreInventoryLine> storeInventoryLines = new HashSet<>();

	@OneToMany(mappedBy = "produit")
	private Set<OrderLine> orderLines = new HashSet<>();

	@OneToMany(mappedBy = "produit")
	private Set<InventoryTransaction> inventoryTransactions = new HashSet<>();

	@ManyToOne
	@JsonIgnoreProperties(value = "produits", allowSetters = true)
	private Categorie categorie;
	@ManyToOne
	@JsonIgnoreProperties(value = "produits", allowSetters = true)
	private Produit parent;
	@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER, cascade = { CascadeType.REMOVE })
	private List<Produit> produits = new ArrayList<>();
	@ManyToOne(optional = false)
	private User user;

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

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

	public String getLibelle() {
		return libelle;
	}

	public Produit libelle(String libelle) {
		this.libelle = libelle;
		return this;
	}



    public void setLibelle(String libelle) {
		this.libelle = libelle;
	}

	public String getCode() {
		return code;
	}

	public Produit code(String code) {
		this.code = code;
		return this;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public Produit imageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
		return this;
	}



	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public TypeProduit getTypeProduit() {
		return typeProduit;
	}

	public Produit typeProduit(TypeProduit typeProduit) {
		this.typeProduit = typeProduit;
		return this;
	}

	public void setTypeProduit(TypeProduit typeProduit) {
		this.typeProduit = typeProduit;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public Produit quantity(Integer quantity) {
		this.quantity = quantity;
		return this;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Integer getCostAmount() {
		return costAmount;
	}

	public Produit costAmount(Integer costAmount) {
		this.costAmount = costAmount;
		return this;
	}

	public void setCostAmount(Integer costAmount) {
		this.costAmount = costAmount;
	}

	public Integer getRegularUnitPrice() {
		return regularUnitPrice;
	}

	public Produit regularUnitPrice(Integer regularUnitPrice) {
		this.regularUnitPrice = regularUnitPrice;
		return this;
	}

	public void setRegularUnitPrice(Integer regularUnitPrice) {
		this.regularUnitPrice = regularUnitPrice;
	}

	public Integer getNetUnitPrice() {
		return netUnitPrice;
	}

	public Produit netUnitPrice(Integer netUnitPrice) {
		this.netUnitPrice = netUnitPrice;
		return this;
	}

	public void setNetUnitPrice(Integer netUnitPrice) {
		this.netUnitPrice = netUnitPrice;
	}

	public Instant getCreatedAt() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		return createdAt;
	}

	public Produit createdAt(Instant createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		if (updatedAt == null) {
			updatedAt = Instant.now();
		}
		return updatedAt;
	}

	public Produit updatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
		return this;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Integer getItemQty() {
		return itemQty;
	}

	public Produit itemQty(Integer itemQty) {
		this.itemQty = itemQty;
		return this;
	}

	public void setItemQty(Integer itemQty) {
		this.itemQty = itemQty;
	}

	public Integer getItemCostAmount() {
		return itemCostAmount;
	}

	public Produit itemCostAmount(Integer itemCostAmount) {
		this.itemCostAmount = itemCostAmount;
		return this;
	}

	public void setItemCostAmount(Integer itemCostAmount) {
		this.itemCostAmount = itemCostAmount;
	}

	public Integer getItemRegularUnitPrice() {
		return itemRegularUnitPrice;
	}

	public Produit itemRegularUnitPrice(Integer itemRegularUnitPrice) {
		this.itemRegularUnitPrice = itemRegularUnitPrice;
		return this;
	}

	public void setItemRegularUnitPrice(Integer itemRegularUnitPrice) {
		this.itemRegularUnitPrice = itemRegularUnitPrice;
	}

	public Set<SalesLine> getSalesLines() {
		return salesLines;
	}

	public Produit salesLines(Set<SalesLine> salesLines) {
		this.salesLines = salesLines;
		return this;
	}

	public Produit addSalesLine(SalesLine salesLine) {
		this.salesLines.add(salesLine);
		salesLine.setProduit(this);
		return this;
	}



	public void setSalesLines(Set<SalesLine> salesLines) {
		this.salesLines = salesLines;
	}

	public Set<StoreInventoryLine> getStoreInventoryLines() {
		return storeInventoryLines;
	}

	public Produit storeInventoryLines(Set<StoreInventoryLine> storeInventoryLines) {
		this.storeInventoryLines = storeInventoryLines;
		return this;
	}

	public Produit addStoreInventoryLine(StoreInventoryLine storeInventoryLine) {
		this.storeInventoryLines.add(storeInventoryLine);
		storeInventoryLine.setProduit(this);
		return this;
	}

	public Produit removeStoreInventoryLine(StoreInventoryLine storeInventoryLine) {
		this.storeInventoryLines.remove(storeInventoryLine);
		storeInventoryLine.setProduit(null);
		return this;
	}

	public void setStoreInventoryLines(Set<StoreInventoryLine> storeInventoryLines) {
		this.storeInventoryLines = storeInventoryLines;
	}

	public Set<OrderLine> getOrderLines() {
		return orderLines;
	}

	public Produit orderLines(Set<OrderLine> orderLines) {
		this.orderLines = orderLines;
		return this;
	}

	public Produit addOrderLine(OrderLine orderLine) {
		this.orderLines.add(orderLine);
		orderLine.setProduit(this);
		return this;
	}

    public Produit addProduit(Produit produit) {
		this.produits.add(produit);
		produit.setParent(this);
		return this;
	}

	public void setOrderLines(Set<OrderLine> orderLines) {
		this.orderLines = orderLines;
	}

	public Set<InventoryTransaction> getInventoryTransactions() {
		return inventoryTransactions;
	}

	public Produit inventoryTransactions(Set<InventoryTransaction> inventoryTransactions) {
		this.inventoryTransactions = inventoryTransactions;
		return this;
	}

	public Produit addInventoryTransaction(InventoryTransaction inventoryTransaction) {
		this.inventoryTransactions.add(inventoryTransaction);
		inventoryTransaction.setProduit(this);
		return this;
	}

	public Produit removeInventoryTransaction(InventoryTransaction inventoryTransaction) {
		this.inventoryTransactions.remove(inventoryTransaction);
		inventoryTransaction.setProduit(null);
		return this;
	}

	public void setInventoryTransactions(Set<InventoryTransaction> inventoryTransactions) {
		this.inventoryTransactions = inventoryTransactions;
	}

	public Categorie getCategorie() {
		return categorie;
	}

	public Produit categorie(Categorie categorie) {
		this.categorie = categorie;
		return this;
	}

	public void setCategorie(Categorie categorie) {
		this.categorie = categorie;
	}

	public Produit getParent() {
		return parent;
	}

	public void setParent(Produit parent) {
		this.parent = parent;
	}

	public List<Produit> getProduits() {

		return produits;
	}

	public static Produit detailFromParent(Produit parent) {
		Produit produit=new Produit();
		produit.setParent(parent);
		produit.setLibelle(parent.getLibelle());
		produit.setCategorie(parent.getCategorie());
		produit.setUpdatedAt(parent.getUpdatedAt());
		produit.setCreatedAt(parent.getCreatedAt());
		produit.setCode(parent.getCode());
		produit.setCostAmount(parent.getItemCostAmount());
		produit.setImageUrl(parent.getImageUrl());
		produit.setItemQty(0);
		produit.setQuantity(0);
		produit.setItemRegularUnitPrice(0);
		produit.setItemCostAmount(0);
		produit.setRegularUnitPrice(parent.getItemRegularUnitPrice());
		produit.setNetUnitPrice(0);
		produit.setTypeProduit(TypeProduit.DETAIL);
		produit.setUser(parent.getUser());
		return produit;
	}
	public static Produit detailFromParent(Produit parent,Produit produit) {
		produit.setCategorie(parent.getCategorie());
		produit.setUpdatedAt(parent.getUpdatedAt());
		produit.setCode(parent.getCode());
		produit.setCostAmount(parent.getItemCostAmount());
		produit.setUser(parent.getUser());
		return produit;
	}
	public void setProduits(List<Produit> produits) {
		this.produits = produits;
	}



	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Produit)) {
			return false;
		}
		return id != null && id.equals(((Produit) o).id);
	}

	@Override
	public int hashCode() {
		return 31;
	}


	@Override
	public String toString() {
		return "Produit{" + "id=" + getId() + ", libelle='" + getLibelle() + "'" + ", code='" + getCode() + "'"
				+ ", imageUrl='" + getImageUrl() + "'" + ", typeProduit='" + getTypeProduit() + "'" + ", quantity="
				+ getQuantity() + ", costAmount=" + getCostAmount() + ", regularUnitPrice=" + getRegularUnitPrice()
				+ ", netUnitPrice=" + getNetUnitPrice() + ", createdAt='" + getCreatedAt() + "'" + ", updatedAt='"
				+ getUpdatedAt() + "'" + ", itemQty=" + getItemQty() + ", itemCostAmount=" + getItemCostAmount()
				+ ", itemRegularUnitPrice=" + getItemRegularUnitPrice() + "}";
	}
}
