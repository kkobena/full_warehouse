package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.service.stock.StockSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service pour le snapshot quotidien de stock.
 *
 * <p>Idempotent — le {@code ON CONFLICT DO NOTHING} en base protège contre les doublons.
 * Appelé par {@link JobOrchestrationService} au démarrage et via le pipeline nocturne.
 */
@Service
public class StockSnapshotSchedulerService {

    private static final Logger LOG = LoggerFactory.getLogger(StockSnapshotSchedulerService.class);

    private final StockSnapshotService stockSnapshotService;

    public StockSnapshotSchedulerService(StockSnapshotService stockSnapshotService) {
        this.stockSnapshotService = stockSnapshotService;
    }

    /**
     * Crée le snapshot stock du jour s'il n'existe pas encore.
     * Idempotent : peut être appelé plusieurs fois sans risque.
     */
    public void createDailySnapshot() {
        LOG.info("Snapshot stock — démarrage");
        stockSnapshotService.createDailySnapshotForAll();
    }
}
