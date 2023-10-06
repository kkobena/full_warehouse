package com.kobe.warehouse.service.dto.records;

import java.math.BigInteger;

public record StoreInventoryLineRecord(
    int produitId,
    String produitCip,
    String produitEan,
    String produitLibelle,
    BigInteger id,
    Integer gap,
    Integer quantityOnHand,
    int quantityInit,
    boolean updated,
    Integer prixAchat,
    Integer prixUni) {}
