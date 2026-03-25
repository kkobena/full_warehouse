package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.service.stock.StockSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduler pour le snapshot quotidien de stock.
 *
 * <p>L'heure d'exécution est configurable via
 * {@code pharma-smart.stock.snapshot.cron} (défaut : 8h30 chaque matin).
 *
 * <p>Les machines n'étant pas forcément allumées 24h/24, un catch-up au
 * démarrage rattrapé automatiquement le snapshot du jour s'il n'a pas encore
 * été produit (idempotent — le ON CONFLICT DO NOTHING protège contre les
 * doublons si la machine était déjà allumée).
 */
@Service
public class StockSnapshotSchedulerService {

    private static final Logger LOG = LoggerFactory.getLogger(StockSnapshotSchedulerService.class);

    private final StockSnapshotService stockSnapshotService;

    public StockSnapshotSchedulerService(StockSnapshotService stockSnapshotService) {
        this.stockSnapshotService = stockSnapshotService;
    }

    /**
     * Exécution planifiée selon {@code pharma-smart.stock.snapshot.cron}.
     * Utiliser '-' dans la config pour désactiver le job.
     */
    @Scheduled(cron = "${pharma-smart.stock.snapshot.cron}")
    public void scheduledSnapshot() {
        LOG.info("Snapshot stock planifié — démarrage");
        stockSnapshotService.createDailySnapshotForAll();
    }

    /**
     * Catch-up au démarrage : crée le snapshot du jour s'il n'existe pas encore.
     * Couvre le cas machine éteinte la nuit / redémarrage en journée.
     * Exécuté en async pour ne pas bloquer le démarrage de l'application.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        LOG.info("Snapshot stock catch-up démarrage");
        try {
            stockSnapshotService.createDailySnapshotForAll();
        } catch (Exception e) {
            LOG.error("Erreur catch-up snapshot au démarrage", e);
        }
    }
}
