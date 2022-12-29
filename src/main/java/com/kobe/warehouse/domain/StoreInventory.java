package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.SalesStatut;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
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
    private SalesStatut statut = SalesStatut.PROCESSING;
    @NotNull
    @ManyToOne(optional = false)
    private Magasin magasin;

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

    public Magasin getMagasin() {
        return magasin;
    }

    public StoreInventory setMagasin(Magasin magasin) {
        this.magasin = magasin;
        return this;
    }

    public Long getInventoryValueCostBegin() {
        return inventoryValueCostBegin;
    }

    public void setInventoryValueCostBegin(Long inventoryValueCostBegin) {
        this.inventoryValueCostBegin = inventoryValueCostBegin;
    }

    public StoreInventory inventoryValueCostBegin(Long inventoryValueCostBegin) {
        this.inventoryValueCostBegin = inventoryValueCostBegin;
        return this;
    }

    public Long getInventoryAmountBegin() {
        return inventoryAmountBegin;
    }

    public void setInventoryAmountBegin(Long inventoryAmountBegin) {
        this.inventoryAmountBegin = inventoryAmountBegin;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public StoreInventory createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public StoreInventory updatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Long getInventoryValueCostAfter() {
        return inventoryValueCostAfter;
    }

    public void setInventoryValueCostAfter(Long inventoryValueCostAfter) {
        this.inventoryValueCostAfter = inventoryValueCostAfter;
    }

    public StoreInventory inventoryValueCostAfter(Long inventoryValueCostAfter) {
        this.inventoryValueCostAfter = inventoryValueCostAfter;
        return this;
    }

    public Long getInventoryAmountAfter() {
        return inventoryAmountAfter;
    }

    public void setInventoryAmountAfter(Long inventoryAmountAfter) {
        this.inventoryAmountAfter = inventoryAmountAfter;
    }

    public StoreInventory inventoryAmountAfter(Long inventoryAmountAfter) {
        this.inventoryAmountAfter = inventoryAmountAfter;
        return this;
    }

    public Set<StoreInventoryLine> getStoreInventoryLines() {
        return storeInventoryLines;
    }

    public void setStoreInventoryLines(Set<StoreInventoryLine> storeInventoryLines) {
        this.storeInventoryLines = storeInventoryLines;
    }

    public StoreInventory storeInventoryLines(Set<StoreInventoryLine> storeInventoryLines) {
        this.storeInventoryLines = storeInventoryLines;
        return this;
    }

    public StoreInventory addStoreInventoryLine(StoreInventoryLine storeInventoryLine) {
        storeInventoryLines.add(storeInventoryLine);
        storeInventoryLine.setStoreInventory(this);
        return this;
    }

    public StoreInventory removeStoreInventoryLine(StoreInventoryLine storeInventoryLine) {
        storeInventoryLines.remove(storeInventoryLine);
        storeInventoryLine.setStoreInventory(null);
        return this;
    }

    public DateDimension getDateDimension() {
        return dateDimension;
    }

    public void setDateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
    }

    public StoreInventory dateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
        return this;
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
