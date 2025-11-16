package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Entity
@Table(name = "historique_inventaire")
public class HistoriqueInventaire implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @NotNull
    @Column(name = "description")
    private String description;

    @NotNull
    @Column(name = "inventory_value_cost_begin", nullable = false)
    private long inventoryValueCostBegin;

    @NotNull
    @Column(name = "inventory_amount_begin", nullable = false)
    private long inventoryAmountBegin;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime created;

    @Column(name = "inventory_value_cost_after", nullable = false)
    private long inventoryValueCostAfter;

    @NotNull
    @Column(name = "inventory_amount_after", nullable = false)
    private long inventoryAmountAfter;

    @Column(name = "gap_cost")
    private long gapCost;

    @Column(name = "gap_amount")
    private long gapAmount;

    public HistoriqueInventaire() {}

    public HistoriqueInventaire(StoreInventory storeInventory) {
        this.id = storeInventory.getId();
        this.description = Objects.isNull(storeInventory.getDescription())
            ? "Inventaire du " + storeInventory.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/YYYY HH:mm:"))
            : storeInventory.getDescription();
        this.inventoryValueCostBegin = storeInventory.getInventoryValueCostBegin();
        this.inventoryAmountBegin = storeInventory.getInventoryAmountBegin();
        this.created = storeInventory.getCreatedAt();
        this.inventoryValueCostAfter = storeInventory.getInventoryValueCostAfter();
        this.inventoryAmountAfter = storeInventory.getInventoryAmountAfter();
        this.gapCost = storeInventory.getGapCost();
        this.gapAmount = storeInventory.getGapAmount();
    }

    public Long getId() {
        return id;
    }

    public HistoriqueInventaire setId(Long id) {
        this.id = id;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public HistoriqueInventaire setDescription(String description) {
        this.description = description;
        return this;
    }

    public long getInventoryValueCostBegin() {
        return inventoryValueCostBegin;
    }

    public HistoriqueInventaire setInventoryValueCostBegin(long inventoryValueCostBegin) {
        this.inventoryValueCostBegin = inventoryValueCostBegin;
        return this;
    }

    public long getInventoryAmountBegin() {
        return inventoryAmountBegin;
    }

    public HistoriqueInventaire setInventoryAmountBegin(long inventoryAmountBegin) {
        this.inventoryAmountBegin = inventoryAmountBegin;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public HistoriqueInventaire setCreated(LocalDateTime createdAt) {
        this.created = createdAt;
        return this;
    }

    public long getInventoryValueCostAfter() {
        return inventoryValueCostAfter;
    }

    public HistoriqueInventaire setInventoryValueCostAfter(long inventoryValueCostAfter) {
        this.inventoryValueCostAfter = inventoryValueCostAfter;
        return this;
    }

    public long getInventoryAmountAfter() {
        return inventoryAmountAfter;
    }

    public HistoriqueInventaire setInventoryAmountAfter(long inventoryAmountAfter) {
        this.inventoryAmountAfter = inventoryAmountAfter;
        return this;
    }

    public long getGapCost() {
        return gapCost;
    }

    public HistoriqueInventaire setGapCost(long gapCost) {
        this.gapCost = gapCost;
        return this;
    }

    public long getGapAmount() {
        return gapAmount;
    }

    public HistoriqueInventaire setGapAmount(long gapAmount) {
        this.gapAmount = gapAmount;
        return this;
    }
    // Override equals, hashCode, and toString methods as needed
}
