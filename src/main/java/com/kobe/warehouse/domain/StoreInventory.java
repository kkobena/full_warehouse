package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.SalesStatut;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A StoreInventory.
 */
@Entity
@Table(name = "store_inventory")
public class StoreInventory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "inventory_value_cost_begin", nullable = false)
    private Long inventoryValueCostBegin;

    @NotNull
    @Column(name = "inventory_amount_begin", nullable = false)
    private Long inventoryAmountBegin;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @NotNull
    @Column(name = "inventory_value_cost_after", nullable = false)
    private Long inventoryValueCostAfter;

    @NotNull
    @Column(name = "inventory_amount_after", nullable = false)
    private Long inventoryAmountAfter;

    @OneToMany(mappedBy = "storeInventory")
    private Set<StoreInventoryLine> storeInventoryLines = new HashSet<>();

    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "storeInventories", allowSetters = true)
    private DateDimension dateDimension;
    @ManyToOne(optional = false)
    private User user;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "statut", nullable = false)
    private SalesStatut statut=SalesStatut.PROCESSING;

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

    public Long getInventoryValueCostBegin() {
        return inventoryValueCostBegin;
    }

    public StoreInventory inventoryValueCostBegin(Long inventoryValueCostBegin) {
        this.inventoryValueCostBegin = inventoryValueCostBegin;
        return this;
    }

    public void setInventoryValueCostBegin(Long inventoryValueCostBegin) {
        this.inventoryValueCostBegin = inventoryValueCostBegin;
    }

    public Long getInventoryAmountBegin() {
        return inventoryAmountBegin;
    }

    public StoreInventory inventoryAmountBegin(Long inventoryAmountBegin) {
        this.inventoryAmountBegin = inventoryAmountBegin;
        return this;
    }

    public SalesStatut getStatut() {
        return statut;
    }

    public void setStatut(SalesStatut statut) {
        this.statut = statut;
    }

    public void setInventoryAmountBegin(Long inventoryAmountBegin) {
        this.inventoryAmountBegin = inventoryAmountBegin;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public StoreInventory createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public StoreInventory updatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getInventoryValueCostAfter() {
        return inventoryValueCostAfter;
    }

    public StoreInventory inventoryValueCostAfter(Long inventoryValueCostAfter) {
        this.inventoryValueCostAfter = inventoryValueCostAfter;
        return this;
    }

    public void setInventoryValueCostAfter(Long inventoryValueCostAfter) {
        this.inventoryValueCostAfter = inventoryValueCostAfter;
    }

    public Long getInventoryAmountAfter() {
        return inventoryAmountAfter;
    }

    public StoreInventory inventoryAmountAfter(Long inventoryAmountAfter) {
        this.inventoryAmountAfter = inventoryAmountAfter;
        return this;
    }

    public void setInventoryAmountAfter(Long inventoryAmountAfter) {
        this.inventoryAmountAfter = inventoryAmountAfter;
    }

    public Set<StoreInventoryLine> getStoreInventoryLines() {
        return storeInventoryLines;
    }

    public StoreInventory storeInventoryLines(Set<StoreInventoryLine> storeInventoryLines) {
        this.storeInventoryLines = storeInventoryLines;
        return this;
    }

    public StoreInventory addStoreInventoryLine(StoreInventoryLine storeInventoryLine) {
        this.storeInventoryLines.add(storeInventoryLine);
        storeInventoryLine.setStoreInventory(this);
        return this;
    }

    public StoreInventory removeStoreInventoryLine(StoreInventoryLine storeInventoryLine) {
        this.storeInventoryLines.remove(storeInventoryLine);
        storeInventoryLine.setStoreInventory(null);
        return this;
    }

    public void setStoreInventoryLines(Set<StoreInventoryLine> storeInventoryLines) {
        this.storeInventoryLines = storeInventoryLines;
    }

    public DateDimension getDateDimension() {
        return dateDimension;
    }

    public StoreInventory dateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
        return this;
    }

    public void setDateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StoreInventory)) {
            return false;
        }
        return id != null && id.equals(((StoreInventory) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "StoreInventory{" +
            "id=" + getId() +
            ", inventoryValueCostBegin=" + getInventoryValueCostBegin() +
            ", inventoryAmountBegin=" + getInventoryAmountBegin() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", inventoryValueCostAfter=" + getInventoryValueCostAfter() +
            ", inventoryAmountAfter=" + getInventoryAmountAfter() +
            "}";
    }
}
