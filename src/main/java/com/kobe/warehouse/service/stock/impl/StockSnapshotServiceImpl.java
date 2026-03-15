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

    // DATE_TRUNC('day') + ON CONFLICT = idempotent : safe à appeler plusieurs fois le même jour.
    private static final String SQL_DAILY_SNAPSHOT = """
        INSERT INTO stock_produit_snapshot
            (produit_id, storage_id, snapshot_date, qty_stock, source_type)
        SELECT sp.produit_id,
               sp.storage_id,
               DATE_TRUNC('day', NOW()),
               sp.qty_stock,
               'BATCH_QUOTIDIEN'
        FROM stock_produit sp
        JOIN storage s ON s.id = sp.storage_id
        WHERE s.magasin_id = :magasinId
        ON CONFLICT ON CONSTRAINT uq_snapshot_produit_storage_date DO NOTHING
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
