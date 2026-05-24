package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.service.dto.enumeration.StoreInventoryExportGroupBy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryExportWrapper {

    private Map<String, InventoryExportSummary> inventoryExportSummaries = new HashMap<>();
    private List<StoreInventoryGroupExport> inventoryGroups;
    private StoreInventoryDTO storeInventory;
    private StoreInventoryExportGroupBy exportGroupBy = StoreInventoryExportGroupBy.RAYON;

    public StoreInventoryDTO getStoreInventory() {
        return storeInventory;
    }

    public void setStoreInventory(StoreInventoryDTO storeInventory) {
        this.storeInventory = storeInventory;
    }

    public List<StoreInventoryGroupExport> getInventoryGroups() {
        return inventoryGroups;
    }

    public void setInventoryGroups(List<StoreInventoryGroupExport> inventoryGroups) {
        this.inventoryGroups = inventoryGroups;
    }

    public Map<String, InventoryExportSummary> getInventoryExportSummaries() {
        return inventoryExportSummaries;
    }

    public void setInventoryExportSummaries(Map<String, InventoryExportSummary> inventoryExportSummaries) {
        this.inventoryExportSummaries = inventoryExportSummaries;
    }

    public StoreInventoryExportGroupBy getExportGroupBy() {
        return exportGroupBy;
    }

    public void setExportGroupBy(StoreInventoryExportGroupBy exportGroupBy) {
        this.exportGroupBy = exportGroupBy != null ? exportGroupBy : StoreInventoryExportGroupBy.RAYON;
    }
}
