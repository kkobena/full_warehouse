package com.kobe.warehouse.service.dto;

/**
 * DTO for dashboard alert counts
 * Contains counts of various alerts that the pharmacy manager needs to monitor
 */
public record DashboardAlertCountDTO(
    Long peremptionCount,      // Produits en voie de péremption
    Long ruptureCount,         // Produits en rupture de stock
    Long entreeCount,          // Nouvelles entrées en stock (dernières 24h)
    Long ajustementCount,      // Ajustements récents (dernières 24h)
    Long prixModifCount        // Modifications de prix récentes (dernières 24h)
) {
    public DashboardAlertCountDTO() {
        this(0L, 0L, 0L, 0L, 0L);
    }
}
