package com.kobe.warehouse.service.dto;

/**
 * DTO for dashboard alert counts
 * Contains counts of various alerts that the pharmacy manager needs to monitor
 */
public record DashboardAlertCountDTO(
    Long peremptionCount,                   // Produits en voie de péremption
    Long ruptureCount,                      // Produits en rupture de stock (StockAlertType.RUPTURE)
    Long entreeCount,                       // Nouvelles entrées en stock (dernières 24h)
    Long ajustementCount,                   // Ajustements récents (dernières 24h)
    Long prixModifCount,                    // Modifications de prix récentes (dernières 24h)
    Long urgentCount,                       // Produits SEMOIS urgents (rupture + sous seuil — à commander)
    Long facturationOverdueCount,           // Factures tiers-payant dont l'échéance de règlement est dépassée
    Long comptesFournisseursOverdueCount    // Commandes fournisseurs dont l'échéance de paiement est dépassée
) {
    public DashboardAlertCountDTO() {
        this(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }
}
