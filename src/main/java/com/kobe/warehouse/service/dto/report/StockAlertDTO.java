package com.kobe.warehouse.service.dto.report;

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
    public enum StockAlertType {
        RUPTURE,    // Out of stock (quantity = 0)
        ALERTE,     // Low stock (quantity < minimum threshold)
        PEREMPTION  // Near expiration (< 3 months)
    }
}
