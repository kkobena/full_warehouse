package com.kobe.warehouse.service.dto.dashboard;

public record StockAlertsDTO(
    Integer rupture,
    Integer stockCritique,
    Integer bientotEnRupture,
    Integer reassortStockRayon
) {
}
