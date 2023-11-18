package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.InventoryType;
import java.time.LocalDateTime;
import java.util.List;

public class StoreInventoryExport {
  private List<StoreInventoryStorageExport> inventoryStorages;
  private Long inventoryValueCostBegin;
  private Long inventoryAmountBegin;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long inventoryValueCostAfter;
  private Long inventoryAmountAfter;
  private String userFullName;
  private String abbrName;
  private String statut;
  private InventoryType inventoryType;
  private CategoryInventory inventoryCategory;
  private String firstName;
  private String lastName;

  public StoreInventoryExport(StoreInventory storeInventory) {

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
    this.inventoryCategory = new CategoryInventory(storeInventory.getInventoryCategory());
  }

  public StoreInventoryExport(
      Long inventoryValueCostBegin,
      Long inventoryValueCostAfter,
      Long inventoryAmountBegin,
      Long inventoryAmountAfter,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String firstName,
      String lastName) {
    this.inventoryValueCostBegin = inventoryValueCostBegin;
    this.inventoryAmountBegin = inventoryAmountBegin;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.inventoryValueCostAfter = inventoryValueCostAfter;
    this.inventoryAmountAfter = inventoryAmountAfter;
    this.abbrName = String.format("%s. %s", firstName.charAt(0), lastName);
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public List<StoreInventoryStorageExport> getInventoryStorages() {
    return inventoryStorages;
  }

  public void setInventoryStorages(List<StoreInventoryStorageExport> inventoryStorages) {
    this.inventoryStorages = inventoryStorages;
  }

  public long getInventoryValueCostBegin() {
    return inventoryValueCostBegin;
  }

  public void setInventoryValueCostBegin(long inventoryValueCostBegin) {
    this.inventoryValueCostBegin = inventoryValueCostBegin;
  }

  public long getInventoryAmountBegin() {
    return inventoryAmountBegin;
  }

  public void setInventoryAmountBegin(long inventoryAmountBegin) {
    this.inventoryAmountBegin = inventoryAmountBegin;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public long getInventoryValueCostAfter() {
    return inventoryValueCostAfter;
  }

  public void setInventoryValueCostAfter(long inventoryValueCostAfter) {
    this.inventoryValueCostAfter = inventoryValueCostAfter;
  }

  public long getInventoryAmountAfter() {
    return inventoryAmountAfter;
  }

  public void setInventoryAmountAfter(long inventoryAmountAfter) {
    this.inventoryAmountAfter = inventoryAmountAfter;
  }

  public String getUserFullName() {
    return userFullName;
  }

  public void setUserFullName(String userFullName) {
    this.userFullName = userFullName;
  }

  public String getAbbrName() {
    return abbrName;
  }

  public void setAbbrName(String abbrName) {
    this.abbrName = abbrName;
  }

  public String getStatut() {
    return statut;
  }

  public void setStatut(String statut) {
    this.statut = statut;
  }

  public InventoryType getInventoryType() {
    return inventoryType;
  }

  public void setInventoryType(InventoryType inventoryType) {
    this.inventoryType = inventoryType;
  }

  public CategoryInventory getInventoryCategory() {
    return inventoryCategory;
  }

  public void setInventoryCategory(CategoryInventory inventoryCategory) {
    this.inventoryCategory = inventoryCategory;
  }
}
