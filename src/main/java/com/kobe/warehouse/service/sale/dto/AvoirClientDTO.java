package com.kobe.warehouse.service.sale.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AvoirClientDTO(
    Long saleId,
    LocalDate saleDate,
    String numberTransaction,
    String customerName,
    String sellerName,
    Long salesLineId,
    String produitLibelle,
    String codeCip,
    int quantityAvoir,
    int regularUnitPrice,
    int netUnitPrice,
    int montantAvoir,
    LocalDateTime updatedAt
) {
}
