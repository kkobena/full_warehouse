package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.service.dto.records.StoreInventoryRecord;

import java.util.Set;

public record CreateInventoryFromProduitIds(
    Set<Integer> produitIds,
    StoreInventoryRecord storeInventoryRecord
) {
}
