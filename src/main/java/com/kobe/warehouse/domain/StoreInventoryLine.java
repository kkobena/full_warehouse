package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

/**
 * A StoreInventoryLine.
 */
@Entity
@Table(name = "store_inventory_line", uniqueConstraints =
    {@UniqueConstraint(columnNames = {"produit_id", "store_inventory_id"})})
public class StoreInventoryLine implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand;
    @Column(name = "gap")
    private Integer gap = 0;
    @NotNull
    @Column(name = "quantity_init", nullable = false)
    private Integer quantityInit;

    @NotNull
    @Column(name = "quantity_sold", nullable = false)
    private Integer quantitySold=0;

    @NotNull
    @Column(name = "inventory_value_cost", nullable = false)
    private Integer inventoryValueCost;

    @NotNull
    @Column(name = "inventory_value_latest_selling_price", nullable = false)
    private Integer inventoryValueLatestSellingPrice=0;

    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "storeInventoryLines", allowSetters = true)
    private StoreInventory storeInventory;
    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt=LocalDateTime.now();
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "storeInventoryLines", allowSetters = true)
    private Produit produit;
    @NotNull
    @Column(name = "updated", nullable = false)
    private Boolean updated = false;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "statut", nullable = false)
    private InventoryStatut statut = InventoryStatut.CREATE;
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public StoreInventoryLine quantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public InventoryStatut getStatut() {
        return statut;
    }

    public Integer getQuantityInit() {
        return quantityInit;
    }

    public void setQuantityInit(Integer quantityInit) {
        this.quantityInit = quantityInit;
    }

    public StoreInventoryLine quantityInit(Integer quantityInit) {
        this.quantityInit = quantityInit;
        return this;
    }

    public Integer getQuantitySold() {
        return quantitySold;
    }

    public void setQuantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
    }

    public StoreInventoryLine quantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
        return this;
    }

    public Integer getInventoryValueCost() {
        return inventoryValueCost;
    }

    public void setInventoryValueCost(Integer inventoryValueCost) {
        this.inventoryValueCost = inventoryValueCost;
    }

    public StoreInventoryLine inventoryValueCost(Integer inventoryValueCost) {
        this.inventoryValueCost = inventoryValueCost;
        return this;
    }

    public Integer getInventoryValueLatestSellingPrice() {
        return inventoryValueLatestSellingPrice;
    }

    public void setInventoryValueLatestSellingPrice(Integer inventoryValueLatestSellingPrice) {
        this.inventoryValueLatestSellingPrice = inventoryValueLatestSellingPrice;
    }

    public StoreInventoryLine inventoryValueLatestSellingPrice(Integer inventoryValueLatestSellingPrice) {
        this.inventoryValueLatestSellingPrice = inventoryValueLatestSellingPrice;
        return this;
    }

    public StoreInventory getStoreInventory() {
        return storeInventory;
    }

    public void setStoreInventory(StoreInventory storeInventory) {
        this.storeInventory = storeInventory;
    }

    public StoreInventoryLine storeInventory(StoreInventory storeInventory) {
        this.storeInventory = storeInventory;
        return this;
    }

    public Integer getGap() {
        return gap;
    }

    public void setGap(Integer gap) {
        this.gap = gap;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public StoreInventoryLine produit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public Boolean getUpdated() {
        return updated;
    }

    public void setUpdated(Boolean updated) {
        this.updated = updated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StoreInventoryLine)) {
            return false;
        }
        return id != null && id.equals(((StoreInventoryLine) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }


    @Override
    public String toString() {
        return "StoreInventoryLine{" +
            "id=" + getId() +
            ", quantityOnHand=" + getQuantityOnHand() +
            ", quantityInit=" + getQuantityInit() +
            ", quantitySold=" + getQuantitySold() +
            ", inventoryValueCost=" + getInventoryValueCost() +
            ", inventoryValueLatestSellingPrice=" + getInventoryValueLatestSellingPrice() +
            "}";
    }
}
