package com.kobe.warehouse.web.rest.dashboard;

import com.kobe.warehouse.service.dashboard.VendeurDashboardService;
import com.kobe.warehouse.service.dto.vendeur.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Vendeur Dashboard
 */
@RestController
@RequestMapping("/api/vendeur/dashboard")
public class VendeurDashboardResource {

    private static final Logger LOG = LoggerFactory.getLogger(VendeurDashboardResource.class);

    private final VendeurDashboardService dashboardService;

    public VendeurDashboardResource(VendeurDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * GET /api/vendeur/dashboard : Get complete dashboard data
     *
     * @return ResponseEntity with VendeurDashboardDTO
     */
    @GetMapping
    public ResponseEntity<VendeurDashboardDTO> getDashboardData() {
        LOG.debug("REST request to get Vendeur Dashboard data");
        VendeurDashboardDTO dashboard = dashboardService.getDashboardData();
        return ResponseEntity.ok(dashboard);
    }

    /**
     * GET /api/vendeur/dashboard/mes-performances : Get seller's performance metrics
     *
     * @return ResponseEntity with MesPerformancesDTO
     */
    @GetMapping("/mes-performances")
    public ResponseEntity<MesPerformancesDTO> getMesPerformances() {
        LOG.debug("REST request to get seller performances");
        MesPerformancesDTO performances = dashboardService.getMesPerformances();
        return ResponseEntity.ok(performances);
    }

    /**
     * GET /api/vendeur/dashboard/mes-clients : Get seller's client statistics
     *
     * @return ResponseEntity with MesClientsDTO
     */
    @GetMapping("/mes-clients")
    public ResponseEntity<MesClientsDTO> getMesClients() {
        LOG.debug("REST request to get seller clients");
        MesClientsDTO clients = dashboardService.getMesClients();
        return ResponseEntity.ok(clients);
    }

    /**
     * GET /api/vendeur/dashboard/ventes-par-type : Get sales by type
     *
     * @return ResponseEntity with VentesParTypeDTO
     */
    @GetMapping("/ventes-par-type")
    public ResponseEntity<VentesParTypeDTO> getVentesParType() {
        LOG.debug("REST request to get sales by type");
        VentesParTypeDTO ventes = dashboardService.getVentesParType();
        return ResponseEntity.ok(ventes);
    }

    /**
     * GET /api/vendeur/dashboard/commission : Get commission information
     *
     * @return ResponseEntity with CommissionDTO
     */
    @GetMapping("/commission")
    public ResponseEntity<CommissionDTO> getCommission() {
        LOG.debug("REST request to get commission");
        CommissionDTO commission = dashboardService.getCommission();
        return ResponseEntity.ok(commission);
    }

    /**
     * GET /api/vendeur/dashboard/top-produits : Get top products sold by this seller
     *
     * @param limit Number of products to return (default: 10)
     * @return ResponseEntity with List of TopProduitVendeurDTO
     */
    @GetMapping("/top-produits")
    public ResponseEntity<List<TopProduitVendeurDTO>> getTopProduits(
        @RequestParam(defaultValue = "10") Integer limit
    ) {
        LOG.debug("REST request to get {} top products", limit);
        List<TopProduitVendeurDTO> produits = dashboardService.getTopProduits(limit);
        return ResponseEntity.ok(produits);
    }

    /**
     * GET /api/vendeur/dashboard/ventes-recentes : Get recent sales
     *
     * @param limit Number of sales to return (default: 10)
     * @return ResponseEntity with List of VenteRecenteVendeurDTO
     */
    @GetMapping("/ventes-recentes")
    public ResponseEntity<List<VenteRecenteVendeurDTO>> getVentesRecentes(
        @RequestParam(defaultValue = "10") Integer limit
    ) {
        LOG.debug("REST request to get {} recent sales", limit);
        List<VenteRecenteVendeurDTO> ventes = dashboardService.getVentesRecentes(limit);
        return ResponseEntity.ok(ventes);
    }

    /**
     * GET /api/vendeur/dashboard/opportunites : Get sales opportunities
     *
     * @return ResponseEntity with List of OpportuniteVenteDTO
     */
    @GetMapping("/opportunites")
    public ResponseEntity<List<OpportuniteVenteDTO>> getOpportunites() {
        LOG.debug("REST request to get sales opportunities");
        List<OpportuniteVenteDTO> opportunites = dashboardService.getOpportunites();
        return ResponseEntity.ok(opportunites);
    }

    /**
     * GET /api/vendeur/dashboard/objectifs-mensuels : Get monthly objectives
     *
     * @return ResponseEntity with List of ObjectifMensuelDTO
     */
    @GetMapping("/objectifs-mensuels")
    public ResponseEntity<List<ObjectifMensuelDTO>> getObjectifsMensuels() {
        LOG.debug("REST request to get monthly objectives");
        List<ObjectifMensuelDTO> objectifs = dashboardService.getObjectifsMensuels();
        return ResponseEntity.ok(objectifs);
    }

    /**
     * GET /api/vendeur/dashboard/clients-fideles : Get loyal clients
     *
     * @param limit Number of clients to return (default: 10)
     * @return ResponseEntity with List of ClientFideleDTO
     */
    @GetMapping("/clients-fideles")
    public ResponseEntity<List<ClientFideleDTO>> getClientsFideles(
        @RequestParam(defaultValue = "10") Integer limit
    ) {
        LOG.debug("REST request to get {} loyal clients", limit);
        List<ClientFideleDTO> clients = dashboardService.getClientsFideles(limit);
        return ResponseEntity.ok(clients);
    }

    /**
     * POST /api/vendeur/dashboard/refresh : Refresh dashboard data
     *
     * @return ResponseEntity with VendeurDashboardDTO
     */
    @PostMapping("/refresh")
    public ResponseEntity<VendeurDashboardDTO> refreshDashboard() {
        LOG.debug("REST request to refresh dashboard");
        VendeurDashboardDTO dashboard = dashboardService.getDashboardData();
        return ResponseEntity.ok(dashboard);
    }
}
