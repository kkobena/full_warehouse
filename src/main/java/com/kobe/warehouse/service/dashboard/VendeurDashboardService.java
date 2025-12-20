package com.kobe.warehouse.service.dashboard;

import com.kobe.warehouse.service.dto.vendeur.*;
import java.util.List;

public interface VendeurDashboardService {

    /**
     * Get complete dashboard data for seller
     * @return VendeurDashboardDTO
     */
    VendeurDashboardDTO getDashboardData();

    /**
     * Get seller's performance metrics
     * @return MesPerformancesDTO
     */
    MesPerformancesDTO getMesPerformances();

    /**
     * Get seller's client statistics
     * @return MesClientsDTO
     */
    MesClientsDTO getMesClients();

    /**
     * Get sales by type (ordonnance, conseil, parapharmacie)
     * @return VentesParTypeDTO
     */
    VentesParTypeDTO getVentesParType();

    /**
     * Get commission information
     * @return CommissionDTO
     */
    CommissionDTO getCommission();

    /**
     * Get top products sold by this seller
     * @param limit Number of products to return
     * @return List of TopProduitVendeurDTO
     */
    List<TopProduitVendeurDTO> getTopProduits(Integer limit);

    /**
     * Get recent sales
     * @param limit Number of sales to return
     * @return List of VenteRecenteVendeurDTO
     */
    List<VenteRecenteVendeurDTO> getVentesRecentes(Integer limit);

    /**
     * Get sales opportunities
     * @return List of OpportuniteVenteDTO
     */
    List<OpportuniteVenteDTO> getOpportunites();

    /**
     * Get monthly objectives
     * @return List of ObjectifMensuelDTO
     */
    List<ObjectifMensuelDTO> getObjectifsMensuels();

    /**
     * Get loyal clients
     * @param limit Number of clients to return
     * @return List of ClientFideleDTO
     */
    List<ClientFideleDTO> getClientsFideles(Integer limit);
}
