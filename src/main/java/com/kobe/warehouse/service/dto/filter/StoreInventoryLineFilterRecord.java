package com.kobe.warehouse.service.dto.filter;

import com.kobe.warehouse.service.dto.enumeration.StoreInventoryLineEnum;
import java.util.Set;
import javax.validation.constraints.NotNull;

public record StoreInventoryLineFilterRecord(
    @NotNull Long storeInventoryId,
    String search,
    Set<Long> storageIds,
    Long rayonId,
    StoreInventoryLineEnum selectedFilter) {}
