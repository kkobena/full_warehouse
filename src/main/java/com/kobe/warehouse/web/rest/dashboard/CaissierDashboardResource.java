package com.kobe.warehouse.web.rest.dashboard;

import com.kobe.warehouse.service.dashboard.CaissierDashboardService;
import com.kobe.warehouse.service.dto.dashboard.CaissierDashboardDTO;
import com.kobe.warehouse.service.dto.dashboard.CaisseStatusDTO;
import com.kobe.warehouse.service.dto.dashboard.LivraisonAttendueDTO;
import com.kobe.warehouse.service.dto.dashboard.ResumeDifferesDTO;
import com.kobe.warehouse.service.dto.dashboard.SessionEncaissementsDTO;
import com.kobe.warehouse.service.dto.dashboard.VenteRecenteDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller du dashboard préparateur en pharmacie.
 * Toutes les données sont filtrées sur l'utilisateur connecté.
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
     * GET /api/caissier/dashboard
     * Toutes les données du dashboard en un seul appel (caisse + encaissements + différés + livraisons + transactions).
     */
    @GetMapping
    public ResponseEntity<CaissierDashboardDTO> getDashboardData() {
        LOG.debug("REST request to get Caissier Dashboard data");
        return ResponseEntity.ok(dashboardService.getDashboardData());
    }

    /**
     * GET /api/caissier/dashboard/caisse-status
     * État de la caisse : fond d'ouverture + espèces théoriques (sans écart).
     */
    @GetMapping("/caisse-status")
    public ResponseEntity<CaisseStatusDTO> getCaisseStatus() {
        LOG.debug("REST request to get caisse status");
        return ResponseEntity.ok(dashboardService.getCaisseStatus());
    }

    /**
     * GET /api/caissier/dashboard/session-encaissements
     * Encaissements filtrés sur le cash_register de l'utilisateur connecté.
     */
    @GetMapping("/session-encaissements")
    public ResponseEntity<SessionEncaissementsDTO> getSessionEncaissements() {
        LOG.debug("REST request to get session encaissements");
        return ResponseEntity.ok(dashboardService.getSessionEncaissements());
    }

    /**
     * GET /api/caissier/dashboard/differes-relance
     * Différés dont l'échéance (sale_date) est <= aujourd'hui et rest_to_pay > 0.
     */
    @GetMapping("/differes-relance")
    public ResponseEntity<ResumeDifferesDTO> getDifferesRelance() {
        LOG.debug("REST request to get differes a relancer");
        return ResponseEntity.ok(dashboardService.getDifferesRelance());
    }

    /**
     * GET /api/caissier/dashboard/livraisons-du-jour
     * Commandes fournisseurs REQUESTED pour aujourd'hui.
     */
    @GetMapping("/livraisons-du-jour")
    public ResponseEntity<List<LivraisonAttendueDTO>> getLivraisonsJour() {
        LOG.debug("REST request to get livraisons du jour");
        return ResponseEntity.ok(dashboardService.getLivraisonsJour());
    }

    /**
     * GET /api/caissier/dashboard/ventes-recentes
     * Dernières ventes de la session courante du caissier connecté.
     *
     * @param limit nombre de résultats (défaut : 8)
     */
    @GetMapping("/ventes-recentes")
    public ResponseEntity<List<VenteRecenteDTO>> getVentesRecentes(
        @RequestParam(defaultValue = "8") Integer limit
    ) {
        LOG.debug("REST request to get {} ventes recentes", limit);
        return ResponseEntity.ok(dashboardService.getVentesRecentes(limit));
    }
}
