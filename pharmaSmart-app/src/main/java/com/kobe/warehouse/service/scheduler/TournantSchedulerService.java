package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.service.stock.impl.PlanningInventaireTournantServiceImpl;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service pour l'inventaire tournant (Cycle Counting).
 *
 * <p>Crée un inventaire pour chaque planning tournant dont la prochaine exécution &lt;= aujourd'hui.
 * Appelé par {@link JobOrchestrationService} au démarrage et via le pipeline nocturne.
 */
@Service
public class TournantSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(TournantSchedulerService.class);

    private final PlanningInventaireTournantServiceImpl planningService;

    public TournantSchedulerService(PlanningInventaireTournantServiceImpl planningService) {
        this.planningService = planningService;
    }

    /**
     * Exécute les plannings tournants échus.
     * Crée un inventaire pour chaque planning dont la prochaine_execution &lt;= aujourd'hui.
     */
    public void executerTournantsEchus() {
        log.info("Démarrage du job inventaire tournant");
        List<Long> inventoryIds = planningService.executerTournantsEchus();
        if (inventoryIds.isEmpty()) {
            log.info("Aucun planning tournant échu aujourd'hui.");
        } else {
            log.info("{} inventaire(s) tournant(s) créé(s) : ids={}", inventoryIds.size(), inventoryIds);
        }
    }
}
