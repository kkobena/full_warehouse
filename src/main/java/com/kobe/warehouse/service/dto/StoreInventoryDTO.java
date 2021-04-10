package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.User;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class StoreInventoryDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private long inventoryValueCostBegin;
    private long inventoryAmountBegin;
    private Instant createdAt;
    private Instant updatedAt;
    private long inventoryValueCostAfter;
    private long inventoryAmountAfter;
    private List<StoreInventoryLineDTO> storeInventoryLines = new ArrayList<>();
    private String userFullName;
    private String statut;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getInventoryValueCostBegin() {
        return inventoryValueCostBegin;
    }

    public void setInventoryValueCostBegin(long inventoryValueCostBegin) {
        this.inventoryValueCostBegin = inventoryValueCostBegin;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public long getInventoryAmountBegin() {
        return inventoryAmountBegin;
    }

    public void setInventoryAmountBegin(long inventoryAmountBegin) {
        this.inventoryAmountBegin = inventoryAmountBegin;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
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

    public List<StoreInventoryLineDTO> getStoreInventoryLines() {
        return storeInventoryLines;
    }

    public void setStoreInventoryLines(List<StoreInventoryLineDTO> storeInventoryLines) {
        this.storeInventoryLines = storeInventoryLines;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public StoreInventoryDTO() {
    }

    public StoreInventoryDTO(StoreInventory storeInventory, List<StoreInventoryLineDTO> storeInventoryLines) {
        this.id = storeInventory.getId();
        this.inventoryValueCostBegin = storeInventory.getInventoryValueCostBegin();
        this.inventoryAmountBegin = storeInventory.getInventoryAmountBegin();
        this.createdAt = storeInventory.getCreatedAt();
        this.updatedAt = storeInventory.getUpdatedAt();
        this.inventoryValueCostAfter = storeInventory.getInventoryValueCostAfter();
        this.inventoryAmountAfter = storeInventory.getInventoryAmountAfter();
        this.storeInventoryLines = storeInventoryLines;
        User user=storeInventory.getUser();
        this.userFullName = user.getFirstName()+" "+user.getLastName();
        this.statut=storeInventory.getStatut().name();
    }
}
