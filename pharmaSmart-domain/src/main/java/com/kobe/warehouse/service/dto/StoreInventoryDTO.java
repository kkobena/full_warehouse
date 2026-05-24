package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.enumeration.InventoryType;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StoreInventoryDTO implements Serializable {

    @Serial
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
    private int gapCost;
    private int gapAmount;
    private String description;

    public StoreInventoryDTO() {}

    public StoreInventoryDTO(StoreInventory storeInventory) {
        this.id = storeInventory.getId();
        this.inventoryValueCostBegin = storeInventory.getInventoryValueCostBegin();
        this.inventoryAmountBegin = storeInventory.getInventoryAmountBegin();
        this.createdAt = storeInventory.getCreatedAt();
        this.updatedAt = storeInventory.getUpdatedAt();
        this.inventoryValueCostAfter = storeInventory.getInventoryValueCostAfter();
        this.inventoryAmountAfter = storeInventory.getInventoryAmountAfter();
        AppUser user = storeInventory.getUser();
        this.abbrName = String.format("%s. %s", user.getFirstName().charAt(0), user.getLastName());
        this.statut = storeInventory.getStatut().name();
        this.inventoryType = storeInventory.getInventoryType();
        this.inventoryCategory = new CategoryInventory(storeInventory.getInventoryCategory());
        if (Objects.nonNull(storeInventory.getStorage())) {
            this.storage = new StorageDTO(storeInventory.getStorage());
        }
        if (Objects.nonNull(storeInventory.getRayon())) {
            this.rayon = new RayonDTO(storeInventory.getRayon());
        }
        if (Objects.nonNull(storeInventory.getGapCost())) {
            this.gapCost = storeInventory.getGapCost();
        }
        if (Objects.nonNull(storeInventory.getGapAmount())) {
            this.gapAmount = storeInventory.getGapAmount();
        }
        this.description = storeInventory.getDescription();
        if (Objects.isNull(storeInventory.getDescription())) {
            this.description = "Inventaire du " + storeInventory.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
    }

    public StoreInventoryDTO(StoreInventory storeInventory, List<StoreInventoryLineDTO> storeInventoryLines) {
        this(storeInventory);
        this.storeInventoryLines = storeInventoryLines;
    }

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

    public List<StoreInventoryLineDTO> getStoreInventoryLines() {
        return storeInventoryLines;
    }

    public void setStoreInventoryLines(List<StoreInventoryLineDTO> storeInventoryLines) {
        this.storeInventoryLines = storeInventoryLines;
    }

    public String getDescription() {
        return description;
    }

    public StoreInventoryDTO setDescription(String description) {
        this.description = description;
        return this;
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

    public StoreInventoryDTO setAbbrName(String abbrName) {
        this.abbrName = abbrName;
        return this;
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

    public StoreInventoryDTO setInventoryType(InventoryType inventoryType) {
        this.inventoryType = inventoryType;
        return this;
    }

    public CategoryInventory getInventoryCategory() {
        return inventoryCategory;
    }

    public StoreInventoryDTO setInventoryCategory(CategoryInventory inventoryCategory) {
        this.inventoryCategory = inventoryCategory;
        return this;
    }

    public StorageDTO getStorage() {
        return storage;
    }

    public StoreInventoryDTO setStorage(StorageDTO storage) {
        this.storage = storage;
        return this;
    }

    public RayonDTO getRayon() {
        return rayon;
    }

    public StoreInventoryDTO setRayon(RayonDTO rayon) {
        this.rayon = rayon;
        return this;
    }

    public int getGapCost() {
        return gapCost;
    }

    public StoreInventoryDTO setGapCost(int gapCost) {
        this.gapCost = gapCost;
        return this;
    }

    public int getGapAmount() {
        return gapAmount;
    }

    public StoreInventoryDTO setGapAmount(int gapAmount) {
        this.gapAmount = gapAmount;
        return this;
    }
}
