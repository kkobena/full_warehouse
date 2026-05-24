package com.kobe.warehouse.service.dto.report;

import com.kobe.warehouse.domain.enumeration.StockAlertType;

import java.time.LocalDate;

public record StockAlertDTO(
    Integer produitId,
    String libelle,
    String codeCip,
    Integer stockQuantity,
    Integer seuilMin,
    LocalDate expiryDate,
    StockAlertType alertType
) {

}
