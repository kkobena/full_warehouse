package com.kobe.warehouse.service.dto.records;

public record StoreInventoryLineRecord(
    int produitId,
    String produitCip,
    String produitEan,
    String produitLibelle,
    Long id,
    Integer gap,
    Integer quantityOnHand,
    int quantityInit,
    boolean updated,
    Integer prixAchat,
    Integer prixUni
) {}
