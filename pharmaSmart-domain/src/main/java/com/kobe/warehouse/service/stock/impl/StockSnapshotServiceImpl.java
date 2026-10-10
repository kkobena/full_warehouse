package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.service.stock.StockSnapshotService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StockSnapshotServiceImpl implements StockSnapshotService {

    private static final Logger LOG = LoggerFactory.getLogger(StockSnapshotServiceImpl.class);

    // Datée à l'instant de la prise : le stock à une date T s'en déduit en ajoutant les mouvements postérieurs, qui ne
    // doivent pas y figurer déjà. Une seule photo quotidienne par ligne de stock : idempotent sur la journée.
    private static final String SQL_DAILY_SNAPSHOT = """
        INSERT INTO stock_produit_snapshot
            (produit_id, storage_id, snapshot_date, qty_stock, qty_ug, source_type)
        SELECT sp.produit_id,
               sp.storage_id,
               NOW(),
               sp.qty_stock,
               sp.qty_ug,
               'BATCH_QUOTIDIEN'
        FROM stock_produit sp
        JOIN storage s ON s.id = sp.storage_id
        WHERE s.magasin_id = :magasinId
          AND NOT EXISTS (SELECT 1
                          FROM stock_produit_snapshot sn
                          WHERE sn.produit_id = sp.produit_id
                            AND sn.storage_id = sp.storage_id
                            AND sn.source_type = 'BATCH_QUOTIDIEN'
                            AND sn.snapshot_date >= DATE_TRUNC('day', NOW()))
        """;

    private final EntityManager em;
    private final MagasinRepository magasinRepository;

    public StockSnapshotServiceImpl(EntityManager em, MagasinRepository magasinRepository) {
        this.em = em;
        this.magasinRepository = magasinRepository;
    }

    @Override
    public void createDailySnapshot(Integer magasinId) {
        int rows = em.createNativeQuery(SQL_DAILY_SNAPSHOT)
            .setParameter("magasinId", magasinId)
            .executeUpdate();
        LOG.debug("Snapshot quotidien magasin={} : {} lignes insérées", magasinId, rows);
    }

    @Override
    public void createDailySnapshotForAll() {
        LOG.info("Démarrage snapshot quotidien stock — tous magasins");
        magasinRepository.findAll().forEach(m -> {
            try {
                createDailySnapshot(m.getId());
            } catch (Exception e) {
                LOG.error("Erreur snapshot magasin={}", m.getId(), e);
            }
        });
        LOG.info("Snapshot quotidien stock terminé");
    }
}
