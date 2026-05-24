package com.kobe.warehouse.service.dto.records;

public record InventoryProgressRecord(
    Long inventoryId,
    long totalLines,
    long updatedLines,
    long linesWithGap,
    int progressPercent
) {}
