package com.kobe.warehouse.service.dto.records;

import java.time.LocalDate;

public record StoreInventoryLotLineRecord(
    Long id,
    Long storeInventoryLineId,
    int produitId,
    String produitCip,
    String produitLibelle,
    String numLot,
    LocalDate expiryDate,
    Integer quantityOnHand,
    Integer quantityInit,
    Integer gap,
    boolean updated,
    String classePareto
) {}
