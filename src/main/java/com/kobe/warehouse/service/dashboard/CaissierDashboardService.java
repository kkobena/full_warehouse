package com.kobe.warehouse.service.dashboard;

import com.kobe.warehouse.service.dto.dashboard.*;
import java.util.List;

public interface CaissierDashboardService {

    /**
     * Get complete dashboard data for cashier
     * @return CaissierDashboardDTO
     */
    CaissierDashboardDTO getDashboardData();

    /**
     * Get today's sales summary
     * @return VentesJourDTO
     */
    VentesJourDTO getVentesJour();

    /**
     * Get cash register status
     * @return CaisseStatusDTO
     */
    CaisseStatusDTO getCaisseStatus();

    /**
     * Get quick statistics
     * @return StatistiquesRapidesDTO
     */
    StatistiquesRapidesDTO getStatistiquesRapides();

    /**
     * Get recent sales
     * @param limit Number of sales to return
     * @return List of VenteRecenteDTO
     */
    List<VenteRecenteDTO> getVentesRecentes(Integer limit);

    /**
     * Get top selling products for today
     * @param limit Number of products to return
     * @return List of TopProduitDTO
     */
    List<TopProduitDTO> getTopProduits(Integer limit);

    /**
     * Get sellers performance
     * @return List of PerformanceVendeurDTO
     */
    List<PerformanceVendeurDTO> getPerformanceVendeurs();

    /**
     * Get alerts for cashier
     * @return List of AlerteCaisseDTO
     */
    List<AlerteCaisseDTO> getAlertes();

    /**
     * Open cash register
     * @param montantOuverture Opening amount
     */
    void ouvrirCaisse(Long montantOuverture);

    /**
     * Close cash register
     */
    void fermerCaisse();
}
