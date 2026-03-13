package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * A InventoryLot.
 */
@Entity
@Table(name = "inventory_lot", uniqueConstraints = {@UniqueConstraint(name = "uq_il_lot_line", columnNames = {"lot_id", "store_inventory_line_id"})})
public class InventoryLot implements Serializable {

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

    @Column(name = "inventory_value_cost")
    private Integer inventoryValueCost;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "store_inventory_line_id", nullable = false)
    private StoreInventoryLine storeInventoryLine;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(optional = false)
    private Lot lot;

    @NotNull
    @Column(name = "updated", nullable = false)
    private Boolean updated = false;

    @Column(name = "last_unit_price")
    private Integer lastUnitPrice;

    public Lot getLot() {
        return lot;
    }

    public void setLot(Lot lot) {
        this.lot = lot;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getLastUnitPrice() {
        return lastUnitPrice;
    }

    public InventoryLot setLastUnitPrice(Integer lastUnitPrice) {
        this.lastUnitPrice = lastUnitPrice;
        return this;
    }

    public Integer getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public InventoryLot quantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public InventoryLot setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Integer getQuantityInit() {
        return quantityInit;
    }

    public void setQuantityInit(Integer quantityInit) {
        this.quantityInit = quantityInit;
    }

    public InventoryLot quantityInit(Integer quantityInit) {
        this.quantityInit = quantityInit;
        return this;
    }



    public Integer getInventoryValueCost() {
        return inventoryValueCost;
    }

    public void setInventoryValueCost(Integer inventoryValueCost) {
        this.inventoryValueCost = inventoryValueCost;
    }

    public InventoryLot inventoryValueCost(Integer inventoryValueCost) {
        this.inventoryValueCost = inventoryValueCost;
        return this;
    }

    public StoreInventoryLine getStoreInventoryLine() {
        return storeInventoryLine;
    }

    public void setStoreInventoryLine(StoreInventoryLine storeInventoryLine) {
        this.storeInventoryLine = storeInventoryLine;
    }

    public InventoryLot storeInventoryLine(StoreInventoryLine storeInventoryLine) {
        this.storeInventoryLine = storeInventoryLine;
        return this;
    }

    public Integer getGap() {
        return gap;
    }

    public void setGap(Integer gap) {
        this.gap = gap;
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
        if (!(o instanceof InventoryLot)) {
            return false;
        }
        return id != null && id.equals(((InventoryLot) o).id);
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
                ", inventoryValueCost=" +
                getInventoryValueCost() +
                ", inventoryValueLatestSellingPrice=" +
                "}"
        );
    }
}
