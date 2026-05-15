package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.service.reassort.SuggestionReassortService;
import com.kobe.warehouse.service.reassort.dto.ReassortRecord;
import com.kobe.warehouse.service.stock.InventoryClosedEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Écoute la clôture d'un inventaire et génère des suggestions de réassort dans les deux sens :
 *
 * <ul>
 *   <li><b>Rayon ← réserve</b> : produits dont le stock rayon &lt; seuil_mini
 *       et la réserve a du stock → suggérer le réassort du rayon.</li>
 *   <li><b>Réserve ← rayon</b> : produits dont la réserve est à 0
 *       et le rayon a du stock → suggérer le réapprovisionnement de la réserve.</li>
 * </ul>
 *
 * <p>Les suggestions ne sont générées que pour les types d'inventaire où le stock a été
 * physiquement vérifié et où un réassort est pertinent ({@link #CATEGORIES_AVEC_REASSORT}).
 * Les types analytiques ou de retrait (PERIME, ALERTE_PEREMPTION, VENDU, INVENDU, EN_RUPTURE)
 * sont exclus.</p>
 *
 * <p>Exécuté de manière asynchrone après le commit de la transaction de clôture.</p>
 */
@Service
public class InventoryClosedEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryClosedEventListener.class);

    /** IN clause SQL des types "réserve" : non-vendables et participant au réassort. */
    private static final String RESERVE_TYPES_IN = Arrays.stream(StorageType.values())
        .filter(t -> !t.isVendable() && t.isReassortSuggere())
        .map(t -> "'" + t.name() + "'")
        .collect(Collectors.joining(", ", "(", ")"));

    /**
     * Types d'inventaire pour lesquels des suggestions de réassort sont pertinentes.
     * Exclus : PERIME, ALERTE_PEREMPTION (but = retrait/retour), VENDU/INVENDU (analytique),
     * EN_RUPTURE (stock total = 0 par construction → aucune suggestion possible).
     */
    private static final Set<InventoryCategory> CATEGORIES_AVEC_REASSORT = Set.of(
        InventoryCategory.MAGASIN,
        InventoryCategory.STORAGE,
        InventoryCategory.RAYON,
        InventoryCategory.FAMILLY,
        InventoryCategory.SOUS_SEUIL,
        InventoryCategory.ABC,
        InventoryCategory.SELECTION_PRODUIT
    );

    /**
     * Produits dont le stock rayon (storage inventorié) &lt; seuil_mini ET qui ont du stock en
     * réserve &gt; 0. → Suggestion : transférer de la réserve vers le rayon.
     */
    private static final String SQL_RAYON_FROM_RESERVE = """
            SELECT sp_rayon.id AS sp_id,
                   (sp_reserve.qty_stock + sp_reserve.qty_ug) AS available_qty
            FROM store_inventory_line sil
            JOIN stock_produit sp_rayon
              ON sp_rayon.produit_id = sil.produit_id
             AND sp_rayon.storage_id = sil.storage_id
            JOIN storage s_reserve
              ON s_reserve.magasin_id = :magasinId
             AND s_reserve.storage_type IN %s
            JOIN stock_produit sp_reserve
              ON sp_reserve.produit_id = sil.produit_id
             AND sp_reserve.storage_id = s_reserve.id
            WHERE sil.store_inventory_id = :inventoryId
              AND (sp_rayon.qty_stock + sp_rayon.qty_ug) < COALESCE(sp_rayon.seuil_mini, 0)
              AND (sp_reserve.qty_stock + sp_reserve.qty_ug) > 0
            """.formatted(RESERVE_TYPES_IN);

    /**
     * Produits dont la réserve est à 0 ET qui ont du stock en rayon (storage
     * inventorié) &gt; 0. → Suggestion : réapprovisionner la réserve depuis le rayon.
     */
    private static final String SQL_RESERVE_FROM_RAYON = """
            SELECT sp_reserve.id AS sp_id,
                   (sp_rayon.qty_stock + sp_rayon.qty_ug) AS available_qty
            FROM store_inventory_line sil
            JOIN stock_produit sp_rayon
              ON sp_rayon.produit_id = sil.produit_id
             AND sp_rayon.storage_id = sil.storage_id
            JOIN storage s_reserve
              ON s_reserve.magasin_id = :magasinId
             AND s_reserve.storage_type IN %s
            JOIN stock_produit sp_reserve
              ON sp_reserve.produit_id = sil.produit_id
             AND sp_reserve.storage_id = s_reserve.id
            WHERE sil.store_inventory_id = :inventoryId
              AND (sp_reserve.qty_stock + sp_reserve.qty_ug) = 0
              AND (sp_rayon.qty_stock + sp_rayon.qty_ug) > 0
            """.formatted(RESERVE_TYPES_IN);

    /**
     * Reconcile lot_stock_location from inventory_lot after close.
     * UPSERT lots with quantity_on_hand > 0, DELETE those at 0.
     * No-op if no inventory_lot records exist (gestion_lot = false).
     */
    private static final String SQL_LOT_LOCATION_UPSERT =
        """
            INSERT INTO lot_stock_location (lot_id, storage_id, qty, updated_at)
            SELECT il.lot_id, :storageId, il.quantity_on_hand, NOW()
            FROM inventory_lot il
            JOIN store_inventory_line sil ON sil.id = il.store_inventory_line_id
            WHERE sil.store_inventory_id = :inventoryId
              AND il.updated  = true
              AND il.quantity_on_hand > 0
            ON CONFLICT (lot_id, storage_id) DO UPDATE
              SET qty = EXCLUDED.qty, updated_at = NOW()
            """;

    private static final String SQL_LOT_LOCATION_DELETE_ZERO =
        """
            DELETE FROM lot_stock_location
            WHERE storage_id = :storageId
              AND lot_id IN (
                SELECT il.lot_id
                FROM inventory_lot il
                JOIN store_inventory_line sil ON sil.id = il.store_inventory_line_id
                WHERE sil.store_inventory_id = :inventoryId
                  AND il.updated = true
                  AND il.quantity_on_hand = 0
              )
            """;

    private final EntityManager em;
    private final SuggestionReassortService suggestionReassortService;

    public InventoryClosedEventListener(
        EntityManager em,
        SuggestionReassortService suggestionReassortService
    ) {
        this.em = em;
        this.suggestionReassortService = suggestionReassortService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onInventoryClosed(InventoryClosedEvent event) {
        if (!CATEGORIES_AVEC_REASSORT.contains(event.inventoryCategory())) {
            log.debug("Inventaire {} ({}) : pas de suggestion de réassort pour ce type",
                event.storeInventoryId(), event.inventoryCategory());
            return;
        }

        AppUser user = em.getReference(AppUser.class, event.userId());

        List<ReassortRecord> allSuggestions = new ArrayList<>();

        // 1. Réassort rayon ← réserve (rayon sous seuil_mini, réserve a du stock)
        allSuggestions.addAll(querySuggestions(SQL_RAYON_FROM_RESERVE, event, "réassort rayon"));

        // 2. Réappro réserve ← rayon (réserve à 0, rayon a du stock)
        allSuggestions.addAll(querySuggestions(SQL_RESERVE_FROM_RAYON, event, "réappro réserve"));

        if (allSuggestions.isEmpty()) {
            log.info("Inventaire {} clôturé : aucune suggestion de réassort",
                event.storeInventoryId());
            return;
        }

        suggestionReassortService.createLigneReassort(allSuggestions, user);
        log.info("Inventaire {} clôturé : {} suggestions créées (rayon + réserve)",
            event.storeInventoryId(), allSuggestions.size());

        reconcileLotStockLocation(event);

        // MAGASIN : la procédure remet la réserve à 0 → purger lot_stock_location de la réserve aussi
        if (event.inventoryCategory() == InventoryCategory.MAGASIN) {
            em.createNativeQuery("""
                    DELETE FROM lot_stock_location lsl
                    USING storage s
                    WHERE lsl.storage_id = s.id
                      AND s.magasin_id   = :magasinId
                      AND s.storage_type IN %s
                    """.formatted(RESERVE_TYPES_IN))
                .setParameter("magasinId", event.magasinId())
                .executeUpdate();
            log.info("Inventaire MAGASIN {} — lot_stock_location réserve purgée", event.storeInventoryId());
        }
    }

    /**
     * Réconcilie {@code lot_stock_location} depuis les {@code inventory_lot} saisis.
     * — UPSERT pour les lots avec quantité > 0
     * — DELETE pour les lots portés à 0
     * Sans effet si l'inventaire n'a pas de gestion_lot (pas d'inventory_lot).
     */
    private void reconcileLotStockLocation(InventoryClosedEvent event) {
        int upserted = em.createNativeQuery(SQL_LOT_LOCATION_UPSERT)
            .setParameter("inventoryId", event.storeInventoryId())
            .setParameter("storageId", event.storageId())
            .executeUpdate();

        int deleted = em.createNativeQuery(SQL_LOT_LOCATION_DELETE_ZERO)
            .setParameter("inventoryId", event.storeInventoryId())
            .setParameter("storageId", event.storageId())
            .executeUpdate();

        log.info("Inventaire {} — lot_stock_location réconcilié : {} upserts, {} suppressions",
            event.storeInventoryId(), upserted, deleted);
    }

    private List<ReassortRecord> querySuggestions(String sql, InventoryClosedEvent event,
        String label) {
        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(sql, Tuple.class)
            .setParameter("inventoryId", event.storeInventoryId())
            .setParameter("magasinId", event.magasinId())
            .getResultList();

        if (rows.isEmpty()) {
            log.debug("Inventaire {} : aucune suggestion de {}", event.storeInventoryId(), label);
            return List.of();
        }

        log.debug("Inventaire {} : {} suggestions de {}", event.storeInventoryId(), rows.size(),
            label);

        return rows.stream()
            .map(t -> {
                Integer spId = ((Number) t.get("sp_id")).intValue();
                Integer availableQty = ((Number) t.get("available_qty")).intValue();
                StockProduit stockProduit = em.getReference(StockProduit.class, spId);
                return new ReassortRecord(stockProduit, availableQty);
            })
            .toList();
    }
}
