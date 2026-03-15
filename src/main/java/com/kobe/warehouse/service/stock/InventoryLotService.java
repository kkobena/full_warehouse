package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.filter.StoreInventoryLineFilterRecord;
import com.kobe.warehouse.service.dto.records.InventoryLotRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryLotLineRecord;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryLotService {

    List<InventoryLotRecord> findByStoreInventoryLineId(Long storeInventoryLineId);

    InventoryLotRecord save(InventoryLotRecord record);

    InventoryLotRecord update(InventoryLotRecord record);

    void delete(Long inventoryLotId);

    Page<StoreInventoryLotLineRecord> findLotFlatPage(StoreInventoryLineFilterRecord filter, Pageable pageable);
}
