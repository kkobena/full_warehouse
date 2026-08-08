package com.kobe.warehouse.service.inventaire.impl;

import com.kobe.warehouse.service.dto.records.InventoryProgressRecord;
import com.kobe.warehouse.service.inventaire.InventaireProgressService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InventaireProgressServiceImpl implements InventaireProgressService {

    /**
     * Progression exprimée dans l'unité que l'opérateur compte réellement à l'écran.
     * <p>Aucune détection de mode n'est nécessaire : sur un inventaire sans lot, la première
     * branche de l'union est vide et le résultat redevient exactement le comptage par ligne.
     */
    private static final String PROGRESS_SQL =
        """
            SELECT COUNT(*)                            AS total,
                   COUNT(*) FILTER (WHERE t.updated)   AS updated,
                   COUNT(*) FILTER (WHERE t.gap <> 0)  AS with_gap
            FROM (
                SELECT COALESCE(il.updated, FALSE) AS updated, COALESCE(il.gap, 0) AS gap
                FROM inventory_lot il
                JOIN store_inventory_line sil ON sil.id = il.store_inventory_line_id
                WHERE sil.store_inventory_id = ?1
                UNION ALL
                SELECT COALESCE(sil.updated, FALSE), COALESCE(sil.gap, 0)
                FROM store_inventory_line sil
                WHERE sil.store_inventory_id = ?1
                  AND NOT EXISTS (
                      SELECT 1 FROM inventory_lot il2
                      WHERE il2.store_inventory_line_id = sil.id
                  )
            ) t
            """;

    private final EntityManager em;

    public InventaireProgressServiceImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public InventoryProgressRecord getProgress(Long inventoryId) {
        Tuple row = (Tuple) em.createNativeQuery(PROGRESS_SQL, Tuple.class)
            .setParameter(1, inventoryId)
            .getSingleResult();

        long total = toLong(row, "total");
        long updated = toLong(row, "updated");
        long withGap = toLong(row, "with_gap");
        int percent = total > 0 ? (int) (updated * 100 / total) : 0;
        return new InventoryProgressRecord(inventoryId, total, updated, withGap, percent);
    }

    /**
     * {@code COUNT} renvoie un {@code bigint} en PostgreSQL, mais le type JDBC exact dépend du
     * dialecte : on normalise plutôt que de caster sur {@code Long}.
     */
    private static long toLong(Tuple tuple, String alias) {
        return tuple.get(alias) instanceof Number n ? n.longValue() : 0L;
    }
}
