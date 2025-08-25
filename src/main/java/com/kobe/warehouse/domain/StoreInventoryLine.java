package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A StoreInventoryLine.
 */

@NamedStoredProcedureQuery(
    name = "StoreInventoryLine.proc_close_inventory",
    procedureName = "proc_close_inventory",
    parameters = {
        @StoredProcedureParameter(mode = ParameterMode.IN, name = "store_inventory_id", type = Long.class),
        @StoredProcedureParameter(mode = ParameterMode.OUT, name = "nombreLigne", type = Integer.class),
    }
)
@Entity
@Table(name = "store_inventory_line", uniqueConstraints = { @UniqueConstraint(columnNames = { "produit_id", "store_inventory_id" }) })
public class StoreInventoryLine implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quantity_on_hand")
    private Integer quantityOnHand;

    @Column(name = "gap")
    private Integer gap = 0;

    @Column(name = "quantity_init")
    private Integer quantityInit;

    @Column(name = "quantity_sold")
    private Integer quantitySold = 0;

    @Column(name = "inventory_value_cost")
    private Integer inventoryValueCost;

    @ManyToOne(optional = false)
    @JoinColumn(name = "store_inventory_id", referencedColumnName = "id")
    private StoreInventory storeInventory;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(optional = false)
    private Produit produit;

    @NotNull
    @Column(name = "updated", nullable = false)
    private Boolean updated = false;

    @Column(name = "last_unit_price")
    private Integer lastUnitPrice;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getLastUnitPrice() {
        return lastUnitPrice;
    }

    public StoreInventoryLine setLastUnitPrice(Integer lastUnitPrice) {
        this.lastUnitPrice = lastUnitPrice;
        return this;
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

    public StoreInventoryLine setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
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
        return (
            "StoreInventoryLine{" +
            "id=" +
            getId() +
            ", quantityOnHand=" +
            getQuantityOnHand() +
            ", quantityInit=" +
            getQuantityInit() +
            ", quantitySold=" +
            getQuantitySold() +
            ", inventoryValueCost=" +
            getInventoryValueCost() +
            ", inventoryValueLatestSellingPrice=" +
            "}"
        );
    }
}
