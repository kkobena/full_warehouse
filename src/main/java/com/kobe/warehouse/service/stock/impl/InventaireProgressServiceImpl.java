package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.service.dto.records.InventoryProgressRecord;
import com.kobe.warehouse.service.stock.InventaireProgressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InventaireProgressServiceImpl implements InventaireProgressService {

    private final StoreInventoryLineRepository storeInventoryLineRepository;

    public InventaireProgressServiceImpl(StoreInventoryLineRepository storeInventoryLineRepository) {
        this.storeInventoryLineRepository = storeInventoryLineRepository;
    }

    @Override
    public InventoryProgressRecord getProgress(Long inventoryId) {
        long total = storeInventoryLineRepository.countByStoreInventoryId(inventoryId);
        long updated = storeInventoryLineRepository.countByStoreInventoryIdAndUpdatedIsTrue(inventoryId);
        long withGap = storeInventoryLineRepository.countByStoreInventoryIdAndGapNot(inventoryId, 0);
        int percent = total > 0 ? (int) (updated * 100 / total) : 0;
        return new InventoryProgressRecord(inventoryId, total, updated, withGap, percent);
    }
}
