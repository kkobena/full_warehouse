package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.stock.impl.PlanningInventaireTournantServiceImpl;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Job planifié pour l'inventaire tournant (Cycle Counting).
 * <p>
 * L'heure d'exécution est configurable via la propriété
 * {@code pharma-smart.inventaire.tournant.cron} (défaut : 8h00 tous les jours).
 * Les officines n'étant pas toutes ouvertes à la même heure et les machines
 * n'étant pas forcément allumées 24h/24, cette valeur peut être ajustée par site.
 */
@Service
public class TournantSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(TournantSchedulerService.class);

    private final PlanningInventaireTournantServiceImpl planningService;

    public TournantSchedulerService(PlanningInventaireTournantServiceImpl planningService) {
        this.planningService = planningService;
    }

    /**
     * Exécution selon le cron configuré ({@code pharma-smart.inventaire.tournant.cron}).
     * Crée un inventaire pour chaque planning tournant dont la prochaine_execution <= aujourd'hui.
     */
    @Scheduled(cron = "${pharma-smart.inventaire.tournant.cron}")
    public void executerTournantsEchus() {
        log.info("Démarrage du job inventaire tournant");
        try {
            List<Long> inventoryIds = planningService.executerTournantsEchus();
            if (inventoryIds.isEmpty()) {
                log.info("Aucun planning tournant échu aujourd'hui.");
            } else {
                log.info("{} inventaire(s) tournant(s) créé(s) : ids={}", inventoryIds.size(), inventoryIds);
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'exécution des inventaires tournants", e);
        }
    }
}
