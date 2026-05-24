package com.kobe.warehouse.service.dto.filter;

import com.kobe.warehouse.service.dto.enumeration.StoreInventoryLineEnum;
import jakarta.validation.constraints.NotNull;

public record StoreInventoryLineFilterRecord(
    @NotNull Long storeInventoryId,
    String search,
    Long storageId,
    Long rayonId,
    StoreInventoryLineEnum selectedFilter
) {}
