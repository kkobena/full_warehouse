package com.kobe.warehouse.service.dto.records;

import java.time.LocalDate;

public record InventoryLotRecord(
    Long id,
    Long storeInventoryLineId,
    Integer lotId,
    String numLot,
    LocalDate expiryDate,
    Integer quantityOnHand,
    Integer quantityInit,
    Integer gap,
    boolean updated,
    Integer lastUnitPrice
) {}
