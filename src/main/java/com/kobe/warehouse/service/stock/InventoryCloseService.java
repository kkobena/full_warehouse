package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.records.ItemsCountRecord;
import com.kobe.warehouse.service.errors.InventoryException;

public interface InventoryCloseService {

    ItemsCountRecord close(Long id) throws InventoryException;
}
