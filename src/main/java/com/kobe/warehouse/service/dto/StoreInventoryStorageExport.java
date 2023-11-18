package com.kobe.warehouse.service.dto;

import java.util.ArrayList;
import java.util.List;

public class StoreInventoryStorageExport {
  private long storageId;
  private String storageLibelle;
  private List<StoreInventoryGroupExport> rayons = new ArrayList<>();

  public StoreInventoryStorageExport() {}

  public StoreInventoryStorageExport(
      long storageId, String strageLibelle, List<StoreInventoryGroupExport> rayons) {
    this.storageId = storageId;
    this.storageLibelle = strageLibelle;
    this.rayons = rayons;
  }

  public long getStorageId() {
    return storageId;
  }

  public void setStorageId(long storageId) {
    this.storageId = storageId;
  }

  public String getStorageLibelle() {
    return storageLibelle;
  }

  public void setStorageLibelle(String storageLibelle) {
    this.storageLibelle = storageLibelle;
  }

  public List<StoreInventoryGroupExport> getRayons() {
    return rayons;
  }

  public void setRayons(List<StoreInventoryGroupExport> rayons) {
    this.rayons = rayons;
  }
}
