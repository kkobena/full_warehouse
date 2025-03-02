package com.kobe.warehouse.service.dto.records;

import java.math.BigDecimal;

public record ProductStatParetoRecord(
    int id,
    String codeCip,
    String codeEan,
    String libelle,
    int quantitySold,
    double quantityAvg,
    double amountAvg,
    BigDecimal netAmount,
    BigDecimal salesAmount,
    BigDecimal htAmount,
    BigDecimal taxAmount
) {}
