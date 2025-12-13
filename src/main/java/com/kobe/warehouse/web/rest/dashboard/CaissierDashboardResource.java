package com.kobe.warehouse.web.rest.dashboard;

import com.kobe.warehouse.service.dashboard.CaissierDashboardService;
import com.kobe.warehouse.service.dto.dashboard.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Caissier Dashboard
 */
@RestController
@RequestMapping("/api/caissier/dashboard")
public class CaissierDashboardResource {

    private static final Logger LOG = LoggerFactory.getLogger(CaissierDashboardResource.class);

    private final CaissierDashboardService dashboardService;

    public CaissierDashboardResource(CaissierDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * GET /api/caissier/dashboard : Get complete dashboard data
     *
     * @return ResponseEntity with CaissierDashboardDTO
     */
    @GetMapping
    public ResponseEntity<CaissierDashboardDTO> getDashboardData() {
        LOG.debug("REST request to get Caissier Dashboard data");
        CaissierDashboardDTO dashboard = dashboardService.getDashboardData();
        return ResponseEntity.ok(dashboard);
    }

    /**
     * GET /api/caissier/dashboard/ventes-jour : Get today's sales summary
     *
     * @return ResponseEntity with VentesJourDTO
     */
    @GetMapping("/ventes-jour")
    public ResponseEntity<VentesJourDTO> getVentesJour() {
        LOG.debug("REST request to get today's sales");
        VentesJourDTO ventes = dashboardService.getVentesJour();
        return ResponseEntity.ok(ventes);
    }

    /**
     * GET /api/caissier/dashboard/caisse-status : Get cash register status
     *
     * @return ResponseEntity with CaisseStatusDTO
     */
    @GetMapping("/caisse-status")
    public ResponseEntity<CaisseStatusDTO> getCaisseStatus() {
        LOG.debug("REST request to get cash register status");
        CaisseStatusDTO status = dashboardService.getCaisseStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * GET /api/caissier/dashboard/statistiques-rapides : Get quick statistics
     *
     * @return ResponseEntity with StatistiquesRapidesDTO
     */
    @GetMapping("/statistiques-rapides")
    public ResponseEntity<StatistiquesRapidesDTO> getStatistiquesRapides() {
        LOG.debug("REST request to get quick statistics");
        StatistiquesRapidesDTO stats = dashboardService.getStatistiquesRapides();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/caissier/dashboard/ventes-recentes : Get recent sales
     *
     * @param limit Number of sales to return (default: 10)
     * @return ResponseEntity with List of VenteRecenteDTO
     */
    @GetMapping("/ventes-recentes")
    public ResponseEntity<List<VenteRecenteDTO>> getVentesRecentes(
        @RequestParam(defaultValue = "10") Integer limit
    ) {
        LOG.debug("REST request to get {} recent sales", limit);
        List<VenteRecenteDTO> ventes = dashboardService.getVentesRecentes(limit);
        return ResponseEntity.ok(ventes);
    }

    /**
     * GET /api/caissier/dashboard/top-produits : Get top selling products
     *
     * @param limit Number of products to return (default: 10)
     * @return ResponseEntity with List of TopProduitDTO
     */
    @GetMapping("/top-produits")
    public ResponseEntity<List<TopProduitDTO>> getTopProduits(
        @RequestParam(defaultValue = "10") Integer limit
    ) {
        LOG.debug("REST request to get {} top products", limit);
        List<TopProduitDTO> produits = dashboardService.getTopProduits(limit);
        return ResponseEntity.ok(produits);
    }

    /**
     * GET /api/caissier/dashboard/performance-vendeurs : Get sellers performance
     *
     * @return ResponseEntity with List of PerformanceVendeurDTO
     */
    @GetMapping("/performance-vendeurs")
    public ResponseEntity<List<PerformanceVendeurDTO>> getPerformanceVendeurs() {
        LOG.debug("REST request to get sellers performance");
        List<PerformanceVendeurDTO> vendeurs = dashboardService.getPerformanceVendeurs();
        return ResponseEntity.ok(vendeurs);
    }

    /**
     * GET /api/caissier/dashboard/alertes : Get alerts
     *
     * @return ResponseEntity with List of AlerteCaisseDTO
     */
    @GetMapping("/alertes")
    public ResponseEntity<List<AlerteCaisseDTO>> getAlertes() {
        LOG.debug("REST request to get alerts");
        List<AlerteCaisseDTO> alertes = dashboardService.getAlertes();
        return ResponseEntity.ok(alertes);
    }

    /**
     * POST /api/caissier/dashboard/refresh : Refresh dashboard data
     *
     * @return ResponseEntity with CaissierDashboardDTO
     */
    @PostMapping("/refresh")
    public ResponseEntity<CaissierDashboardDTO> refreshDashboard() {
        LOG.debug("REST request to refresh dashboard");
        CaissierDashboardDTO dashboard = dashboardService.getDashboardData();
        return ResponseEntity.ok(dashboard);
    }

    /**
     * POST /api/caissier/dashboard/ouvrir-caisse : Open cash register
     *
     * @param request Map with montantOuverture
     * @return ResponseEntity
     */
    @PostMapping("/ouvrir-caisse")
    public ResponseEntity<Void> ouvrirCaisse(@RequestBody Map<String, Long> request) {
        LOG.debug("REST request to open cash register");
        Long montantOuverture = request.get("montantOuverture");
        dashboardService.ouvrirCaisse(montantOuverture);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/caissier/dashboard/fermer-caisse : Close cash register
     *
     * @return ResponseEntity
     */
    @PostMapping("/fermer-caisse")
    public ResponseEntity<Void> fermerCaisse() {
        LOG.debug("REST request to close cash register");
        dashboardService.fermerCaisse();
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/caissier/dashboard/imprimer-rapport : Print cash register report
     *
     * @return ResponseEntity with PDF report
     */
    @GetMapping("/imprimer-rapport")
    public ResponseEntity<byte[]> imprimerRapport() {
        LOG.debug("REST request to print cash register report");
        // TODO: Implement PDF report generation
        return ResponseEntity.ok()
            .header("Content-Type", "application/pdf")
            .body(new byte[0]);
    }
}
