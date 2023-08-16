package com.kobe.warehouse.service.dto.records;

import java.math.BigDecimal;

public record ProductStatRecord(
    int id,
    int produitCount,
    String codeCip,
    String codeEan,
    String libelle,
    int quantitySold,
    int quantityUg,
    BigDecimal netAmount,
    BigDecimal costAmount,
    BigDecimal salesAmount,
    BigDecimal discountAmount,
    BigDecimal montantTvaUg,
    BigDecimal discountAmountHorsUg,
    BigDecimal amountToBeTakenIntoAaccount,
    BigDecimal taxAmount,
    BigDecimal htAmount) {}
