package com.kobe.warehouse.service.sale.dto;

import java.time.LocalDate;

public record SaleLineForRetourDTO(
    Long salesLineId,
    LocalDate salesLineDate,
    String produitLibelle,
    String codeCip,
    int quantitySold,
    int netUnitPrice
) {}
