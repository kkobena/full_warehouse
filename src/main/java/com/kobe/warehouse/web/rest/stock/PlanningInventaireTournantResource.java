package com.kobe.warehouse.web.rest.stock;

import com.kobe.warehouse.service.dto.records.PlanningInventaireTournantRecord;
import com.kobe.warehouse.service.dto.records.TournantDashboardRecord;
import com.kobe.warehouse.service.stock.PlanningInventaireTournantService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for {@link com.kobe.warehouse.domain.PlanningInventaireTournant}.
 */
@RestController
@RequestMapping("/api")
public class PlanningInventaireTournantResource {

    private static final Logger log = LoggerFactory.getLogger(
        PlanningInventaireTournantResource.class);

    private final PlanningInventaireTournantService planningService;

    public PlanningInventaireTournantResource(PlanningInventaireTournantService planningService) {
        this.planningService = planningService;
    }

    /**
     * Lister tous les plannings tournants.
     */
    @GetMapping("/plannings-tournants")
    public ResponseEntity<List<PlanningInventaireTournantRecord>> findAll() {
        return ResponseEntity.ok(planningService.findAll());
    }

    /**
     * Récupérer un planning par id.
     */
    @GetMapping("/plannings-tournants/{id}")
    public ResponseEntity<PlanningInventaireTournantRecord> findById(@PathVariable Integer id) {
        return planningService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Créer un nouveau planning.
     */
    @PostMapping("/plannings-tournants")
    public ResponseEntity<PlanningInventaireTournantRecord> create(
        @Valid @RequestBody PlanningInventaireTournantRecord record
    ) {
        log.debug("REST request to create planning tournant : {}", record);
        return ResponseEntity.ok(planningService.create(record));
    }

    /**
     * Mettre à jour un planning.
     */
    @PutMapping("/plannings-tournants/{id}")
    public ResponseEntity<PlanningInventaireTournantRecord> update(
        @PathVariable Integer id,
        @Valid @RequestBody PlanningInventaireTournantRecord record
    ) {
        log.debug("REST request to update planning tournant id={}", id);
        return ResponseEntity.ok(planningService.update(record));
    }

    /**
     * Supprimer un planning.
     */
    @DeleteMapping("/plannings-tournants/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        log.debug("REST request to delete planning tournant id={}", id);
        planningService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activer / désactiver un planning.
     */
    @PutMapping("/plannings-tournants/{id}/toggle")
    public ResponseEntity<PlanningInventaireTournantRecord> toggleActif(@PathVariable Integer id) {
        return ResponseEntity.ok(planningService.toggleActif(id));
    }

    /**
     * Exécuter manuellement un planning (crée l'inventaire immédiatement).
     *
     * @return l'id du StoreInventory créé
     */
    @PostMapping("/plannings-tournants/{id}/executer")
    public ResponseEntity<Map<String, Long>> executerManuellement(@PathVariable Integer id) {
        Long inventoryId = planningService.executerManuellement(id);
        return ResponseEntity.ok(Map.of("inventoryId", inventoryId));
    }

    /**
     * Dashboard inventaire tournant pour un storage donné (optionnel).
     */
    @GetMapping("/plannings-tournants/dashboard")
    public ResponseEntity<TournantDashboardRecord> getDashboard(
        @RequestParam(required = false) Integer storageId
    ) {
        return ResponseEntity.ok(planningService.getDashboard(storageId));
    }
}
