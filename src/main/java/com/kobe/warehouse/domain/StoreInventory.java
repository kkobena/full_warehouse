package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import com.kobe.warehouse.domain.enumeration.InventoryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A StoreInventory.
 */

@Entity
@Table(name = "store_inventory")
public class StoreInventory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "description")
    private String description;

    @NotNull
    @Column(name = "inventory_value_cost_begin", nullable = false)
    private Long inventoryValueCostBegin;

    @NotNull
    @Column(name = "inventory_amount_begin", nullable = false)
    private Long inventoryAmountBegin;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @NotNull
    @Column(name = "inventory_value_cost_after", nullable = false)
    private Long inventoryValueCostAfter;

    @NotNull
    @Column(name = "inventory_amount_after", nullable = false)
    private Long inventoryAmountAfter;

    @OneToMany(mappedBy = "storeInventory")
    private List<StoreInventoryLine> storeInventoryLines = new ArrayList<>();

    @NotNull
    @ManyToOne(optional = false)
    private AppUser user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private InventoryStatut statut = InventoryStatut.CREATE;

    @ManyToOne
    private Storage storage;

    @ManyToOne
    private Rayon rayon;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_type", nullable = false)
    private InventoryType inventoryType = InventoryType.MANUEL;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_category", nullable = false)
    private InventoryCategory inventoryCategory = InventoryCategory.MAGASIN;

    @Column(name = "gap_cost")
    private Integer gapCost;

    @Column(name = "gap_amount")
    private Integer gapAmount;

    public String getDescription() {
        return description;
    }

    public StoreInventory setDescription(String description) {
        this.description = description;
        return this;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotNull Long getInventoryValueCostBegin() {
        return inventoryValueCostBegin;
    }

    public void setInventoryValueCostBegin(Long inventoryValueCostBegin) {
        this.inventoryValueCostBegin = inventoryValueCostBegin;
    }

    public @NotNull Long getInventoryAmountBegin() {
        return inventoryAmountBegin;
    }

    public void setInventoryAmountBegin(Long inventoryAmountBegin) {
        this.inventoryAmountBegin = inventoryAmountBegin;
    }

    public @NotNull LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public @NotNull LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public @NotNull Long getInventoryValueCostAfter() {
        return inventoryValueCostAfter;
    }

    public void setInventoryValueCostAfter(Long inventoryValueCostAfter) {
        this.inventoryValueCostAfter = inventoryValueCostAfter;
    }

    public @NotNull Long getInventoryAmountAfter() {
        return inventoryAmountAfter;
    }

    public void setInventoryAmountAfter(Long inventoryAmountAfter) {
        this.inventoryAmountAfter = inventoryAmountAfter;
    }

    public List<StoreInventoryLine> getStoreInventoryLines() {
        return storeInventoryLines;
    }

    public void setStoreInventoryLines(List<StoreInventoryLine> storeInventoryLines) {
        this.storeInventoryLines = storeInventoryLines;
    }

    public @NotNull AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public @NotNull InventoryStatut getStatut() {
        return statut;
    }

    public void setStatut(InventoryStatut statut) {
        this.statut = statut;
    }

    public Storage getStorage() {
        return storage;
    }

    public StoreInventory setStorage(Storage storage) {
        this.storage = storage;
        return this;
    }

    public Rayon getRayon() {
        return rayon;
    }

    public StoreInventory setRayon(Rayon rayon) {
        this.rayon = rayon;
        return this;
    }

    public @NotNull InventoryType getInventoryType() {
        return inventoryType;
    }

    public StoreInventory setInventoryType(InventoryType inventoryType) {
        this.inventoryType = inventoryType;
        return this;
    }

    public @NotNull InventoryCategory getInventoryCategory() {
        return inventoryCategory;
    }

    public StoreInventory setInventoryCategory(InventoryCategory inventoryCategory) {
        this.inventoryCategory = inventoryCategory;
        return this;
    }

    public Integer getGapCost() {
        return gapCost;
    }

    public StoreInventory setGapCost(Integer gapCost) {
        this.gapCost = gapCost;
        return this;
    }

    public Integer getGapAmount() {
        return gapAmount;
    }

    public StoreInventory setGapAmount(Integer gapAmount) {
        this.gapAmount = gapAmount;
        return this;
    }

    public StoreInventory inventoryValueCostBegin(Long inventoryValueCostBegin) {
        this.inventoryValueCostBegin = inventoryValueCostBegin;
        return this;
    }

    public StoreInventory inventoryAmountBegin(Long inventoryAmountBegin) {
        this.inventoryAmountBegin = inventoryAmountBegin;
        return this;
    }

    public StoreInventory createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public StoreInventory updatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public StoreInventory inventoryValueCostAfter(Long inventoryValueCostAfter) {
        this.inventoryValueCostAfter = inventoryValueCostAfter;
        return this;
    }

    public StoreInventory inventoryAmountAfter(Long inventoryAmountAfter) {
        this.inventoryAmountAfter = inventoryAmountAfter;
        return this;
    }

    public StoreInventory storeInventoryLines(List<StoreInventoryLine> storeInventoryLines) {
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
        return "StoreInventory{"
            + "id="
            + getId()
            + ", inventoryValueCostBegin="
            + getInventoryValueCostBegin()
            + ", inventoryAmountBegin="
            + getInventoryAmountBegin()
            + ", createdAt='"
            + getCreatedAt()
            + "'"
            + ", updatedAt='"
            + getUpdatedAt()
            + "'"
            + ", inventoryValueCostAfter="
            + getInventoryValueCostAfter()
            + ", inventoryAmountAfter="
            + getInventoryAmountAfter()
            + "}";
    }
}
