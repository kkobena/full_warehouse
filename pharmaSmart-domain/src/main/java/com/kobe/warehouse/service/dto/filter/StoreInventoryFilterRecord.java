package com.kobe.warehouse.service.dto.filter;

import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import java.util.Set;

public record StoreInventoryFilterRecord(
    Set<InventoryCategory> inventoryCategories,
    Set<InventoryStatut> statuts,
    Long storageId,
    Long rayonId,
    Long userId
) {}
