package com.kobe.warehouse.service.dto.dashboard;

import java.util.List;

public record CaissierDashboardDTO(
    VentesJourDTO ventesJour,
    CaisseStatusDTO caisseStatus,
    StatistiquesRapidesDTO statistiquesRapides,
    List<VenteRecenteDTO> ventesRecentes,
    List<TopProduitDTO> topProduits,
    List<PerformanceVendeurDTO> performanceVendeurs,
    List<AlerteCaisseDTO> alertes
) {}
