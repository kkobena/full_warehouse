package com.kobe.warehouse.web.rest.dashboard;

import com.kobe.warehouse.service.dashboard.ResponsableCommandeDashboardService;
import com.kobe.warehouse.service.dto.dashboard.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/responsable-commande/dashboard")
public class ResponsableCommandeDashboardResource {

    private final ResponsableCommandeDashboardService dashboardService;

    public ResponsableCommandeDashboardResource(ResponsableCommandeDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * GET /api/responsable-commande/dashboard : Récupère les données complètes du dashboard
     */
    @GetMapping
    public ResponseEntity<ResponsableCommandeDashboardDTO> getDashboardData() {
        ResponsableCommandeDashboardDTO dashboard = dashboardService.getDashboardData();
        return ResponseEntity.ok(dashboard);
    }

    /**
     * GET /api/responsable-commande/dashboard/stock-alerts : Récupère les alertes stock
     */
    @GetMapping("/stock-alerts")
    public ResponseEntity<StockAlertsDTO> getStockAlerts() {
        StockAlertsDTO alerts = dashboardService.getStockAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * GET /api/responsable-commande/dashboard/commandes-en-cours : Récupère les commandes en cours
     */
    @GetMapping("/commandes-en-cours")
    public ResponseEntity<CommandesEnCoursDTO> getCommandesEnCours() {
        CommandesEnCoursDTO commandes = dashboardService.getCommandesEnCours();
        return ResponseEntity.ok(commandes);
    }

    /**
     * GET /api/responsable-commande/dashboard/peremptions : Récupère les péremptions proches
     */
    @GetMapping("/peremptions")
    public ResponseEntity<PeremptionsDTO> getPeremptions() {
        PeremptionsDTO peremptions = dashboardService.getPeremptions();
        return ResponseEntity.ok(peremptions);
    }

    /**
     * GET /api/responsable-commande/dashboard/rotation-stock : Récupère le taux de rotation
     */
    @GetMapping("/rotation-stock")
    public ResponseEntity<RotationStockDTO> getRotationStock() {
        RotationStockDTO rotation = dashboardService.getRotationStock();
        return ResponseEntity.ok(rotation);
    }

    /**
     * GET /api/responsable-commande/dashboard/suggestions : Récupère les suggestions de réapprovisionnement
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<SuggestionReapproDTO>> getSuggestionsReappro() {
        List<SuggestionReapproDTO> suggestions = dashboardService.getSuggestionsReappro();
        return ResponseEntity.ok(suggestions);
    }

    /**
     * GET /api/responsable-commande/dashboard/analyse-abc : Récupère l'analyse ABC
     */
    @GetMapping("/analyse-abc")
    public ResponseEntity<AnalyseABCDTO> getAnalyseABC() {
        AnalyseABCDTO analyseABC = dashboardService.getAnalyseABC();
        return ResponseEntity.ok(analyseABC);
    }

    /**
     * GET /api/responsable-commande/dashboard/performance-fournisseurs : Récupère les performances fournisseurs
     */
    @GetMapping("/performance-fournisseurs")
    public ResponseEntity<List<PerformanceFournisseurDTO>> getPerformanceFournisseurs(
        @RequestParam(defaultValue = "5") Integer top
    ) {
        List<PerformanceFournisseurDTO> performances = dashboardService.getPerformanceFournisseurs(top);
        return ResponseEntity.ok(performances);
    }

    /**
     * POST /api/responsable-commande/dashboard/refresh : Rafraîchit les données du dashboard
     */
    @PostMapping("/refresh")
    public ResponseEntity<ResponsableCommandeDashboardDTO> refreshDashboard() {
        ResponsableCommandeDashboardDTO dashboard = dashboardService.getDashboardData();
        return ResponseEntity.ok(dashboard);
    }
}
