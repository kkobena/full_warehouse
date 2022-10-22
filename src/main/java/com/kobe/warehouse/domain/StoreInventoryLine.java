package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;

/**
 * A StoreInventoryLine.
 */
@Entity
@Table(name = "store_inventory_line", uniqueConstraints =
    { @UniqueConstraint(columnNames = { "produit_id", "store_inventory_id" }) })
public class StoreInventoryLine implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand;
    @Column(name = "gap")
   private  Integer gap=0;
    @NotNull
    @Column(name = "quantity_init", nullable = false)
    private Integer quantityInit;

    @NotNull
    @Column(name = "quantity_sold", nullable = false)
    private Integer quantitySold;

    @NotNull
    @Column(name = "inventory_value_cost", nullable = false)
    private Integer inventoryValueCost;

    @NotNull
    @Column(name = "inventory_value_latest_selling_price", nullable = false)
    private Integer inventoryValueLatestSellingPrice;

    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "storeInventoryLines", allowSetters = true)
    private StoreInventory storeInventory;

    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "storeInventoryLines", allowSetters = true)
    private Produit produit;
    @NotNull
    @Column(name = "updated", nullable = false)
    private Boolean updated=false;
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuantityOnHand() {
        return quantityOnHand;
    }

    public StoreInventoryLine quantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
        return this;
    }

    public void setQuantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public Integer getQuantityInit() {
        return quantityInit;
    }

    public StoreInventoryLine quantityInit(Integer quantityInit) {
        this.quantityInit = quantityInit;
        return this;
    }

    public void setQuantityInit(Integer quantityInit) {
        this.quantityInit = quantityInit;
    }

    public Integer getQuantitySold() {
        return quantitySold;
    }

    public StoreInventoryLine quantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
        return this;
    }

    public void setQuantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
    }

    public Integer getInventoryValueCost() {
        return inventoryValueCost;
    }

    public StoreInventoryLine inventoryValueCost(Integer inventoryValueCost) {
        this.inventoryValueCost = inventoryValueCost;
        return this;
    }

    public void setInventoryValueCost(Integer inventoryValueCost) {
        this.inventoryValueCost = inventoryValueCost;
    }

    public Integer getInventoryValueLatestSellingPrice() {
        return inventoryValueLatestSellingPrice;
    }

    public StoreInventoryLine inventoryValueLatestSellingPrice(Integer inventoryValueLatestSellingPrice) {
        this.inventoryValueLatestSellingPrice = inventoryValueLatestSellingPrice;
        return this;
    }

    public void setInventoryValueLatestSellingPrice(Integer inventoryValueLatestSellingPrice) {
        this.inventoryValueLatestSellingPrice = inventoryValueLatestSellingPrice;
    }

    public StoreInventory getStoreInventory() {
        return storeInventory;
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

    public void setStoreInventory(StoreInventory storeInventory) {
        this.storeInventory = storeInventory;
    }

    public Produit getProduit() {
        return produit;
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

    public void setProduit(Produit produit) {
        this.produit = produit;
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
