package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.InventoryType;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

@Getter
public class StoreInventoryDTO implements Serializable {
  private static final long serialVersionUID = 1L;
  private Long id;
  private long inventoryValueCostBegin;
  private long inventoryAmountBegin;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private long inventoryValueCostAfter;
  private long inventoryAmountAfter;
  private List<StoreInventoryLineDTO> storeInventoryLines = new ArrayList<>();
  private String userFullName;
  private String abbrName;
  private String statut;
  private InventoryType inventoryType;
  private CategoryInventory inventoryCategory;
  private StorageDTO storage;
  private RayonDTO rayon;

  public StoreInventoryDTO() {}

  public StoreInventoryDTO(StoreInventory storeInventory) {
    this.id = storeInventory.getId();
    this.inventoryValueCostBegin = storeInventory.getInventoryValueCostBegin();
    this.inventoryAmountBegin = storeInventory.getInventoryAmountBegin();
    this.createdAt = storeInventory.getCreatedAt();
    this.updatedAt = storeInventory.getUpdatedAt();
    this.inventoryValueCostAfter = storeInventory.getInventoryValueCostAfter();
    this.inventoryAmountAfter = storeInventory.getInventoryAmountAfter();
    User user = storeInventory.getUser();
    this.abbrName = String.format("%s. %s", user.getFirstName().charAt(0), user.getLastName());
    this.statut = storeInventory.getStatut().name();
    this.inventoryType = storeInventory.getInventoryType();
    this.inventoryCategory = new CategoryInventory(storeInventory.getInventoryCategory()) ;
    if (Objects.nonNull(storeInventory.getStorage())) {
      this.storage = new StorageDTO(storeInventory.getStorage());
    }
    if (Objects.nonNull(storeInventory.getRayon())) {
      this.rayon = new RayonDTO(storeInventory.getRayon());
    }
  }

  public StoreInventoryDTO(
      StoreInventory storeInventory, List<StoreInventoryLineDTO> storeInventoryLines) {
    this(storeInventory);
    this.storeInventoryLines = storeInventoryLines;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setInventoryValueCostBegin(long inventoryValueCostBegin) {
    this.inventoryValueCostBegin = inventoryValueCostBegin;
  }

  public void setStatut(String statut) {
    this.statut = statut;
  }

  public void setInventoryAmountBegin(long inventoryAmountBegin) {
    this.inventoryAmountBegin = inventoryAmountBegin;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public void setInventoryValueCostAfter(long inventoryValueCostAfter) {
    this.inventoryValueCostAfter = inventoryValueCostAfter;
  }

  public void setInventoryAmountAfter(long inventoryAmountAfter) {
    this.inventoryAmountAfter = inventoryAmountAfter;
  }

  public void setStoreInventoryLines(List<StoreInventoryLineDTO> storeInventoryLines) {
    this.storeInventoryLines = storeInventoryLines;
  }

  public void setUserFullName(String userFullName) {
    this.userFullName = userFullName;
  }


}
