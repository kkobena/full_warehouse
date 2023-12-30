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
import lombok.Getter;

/** A StoreInventory. */
@Getter
@Entity
@Table(name = "store_inventory")
public class StoreInventory implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

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
  private User user;

  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "statut", nullable = false)
  private InventoryStatut statut = InventoryStatut.CREATE;

  @ManyToOne private Storage storage;
  @ManyToOne private Rayon rayon;

  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "inventory_type", nullable = false)
  private InventoryType inventoryType = InventoryType.MANUEL;

  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "inventory_category", nullable = false)
  private InventoryCategory inventoryCategory = InventoryCategory.MAGASIN;

  @Column(name = "gap_cost")
  private Integer gapCost;

  @Column(name = "gap_amount")
  private Integer gapAmount;

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

  public void setUser(User user) {
    this.user = user;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setInventoryValueCostBegin(Long inventoryValueCostBegin) {
    this.inventoryValueCostBegin = inventoryValueCostBegin;
  }

  public StoreInventory inventoryValueCostBegin(Long inventoryValueCostBegin) {
    this.inventoryValueCostBegin = inventoryValueCostBegin;
    return this;
  }

  public void setInventoryAmountBegin(Long inventoryAmountBegin) {
    this.inventoryAmountBegin = inventoryAmountBegin;
  }

  public StoreInventory inventoryAmountBegin(Long inventoryAmountBegin) {
    this.inventoryAmountBegin = inventoryAmountBegin;
    return this;
  }

  public void setStatut(InventoryStatut statut) {
    this.statut = statut;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public StoreInventory createdAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public StoreInventory updatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public void setInventoryValueCostAfter(Long inventoryValueCostAfter) {
    this.inventoryValueCostAfter = inventoryValueCostAfter;
  }

  public StoreInventory inventoryValueCostAfter(Long inventoryValueCostAfter) {
    this.inventoryValueCostAfter = inventoryValueCostAfter;
    return this;
  }

  public void setInventoryAmountAfter(Long inventoryAmountAfter) {
    this.inventoryAmountAfter = inventoryAmountAfter;
  }

  public StoreInventory inventoryAmountAfter(Long inventoryAmountAfter) {
    this.inventoryAmountAfter = inventoryAmountAfter;
    return this;
  }

  public void setStoreInventoryLines(List<StoreInventoryLine> storeInventoryLines) {
    this.storeInventoryLines = storeInventoryLines;
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

  public StoreInventory setStorage(Storage storage) {
    this.storage = storage;
    return this;
  }

  public StoreInventory setRayon(Rayon rayon) {
    this.rayon = rayon;
    return this;
  }

  public StoreInventory setInventoryType(InventoryType inventoryType) {
    this.inventoryType = inventoryType;
    return this;
  }

  public StoreInventory setInventoryCategory(InventoryCategory inventoryCategory) {
    this.inventoryCategory = inventoryCategory;
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
