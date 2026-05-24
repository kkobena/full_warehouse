package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.records.StoreInventorySummaryByGroupRecord;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryRecord;
import java.util.List;

public interface InventoryValuationService {

    /** Résumé global (avant / après / écart). */
    StoreInventorySummaryRecord getGlobalSummary(Long inventoryId);

    /**
     * Ventilation par groupe.
     *
     * @param groupBy STORAGE | FAMILLE | RAYON
     */
    List<StoreInventorySummaryByGroupRecord> getSummaryByGroup(Long inventoryId, String groupBy);
}
