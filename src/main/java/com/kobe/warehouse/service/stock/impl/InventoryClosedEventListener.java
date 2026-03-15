package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.service.reassort.SuggestionReassortService;
import com.kobe.warehouse.service.reassort.dto.ReassortRecord;
import com.kobe.warehouse.service.stock.InventoryClosedEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
     * réserve (SAFETY_STOCK) &gt; 0. → Suggestion : transférer de la réserve vers le rayon.
     */
    private static final String SQL_RAYON_FROM_RESERVE =
        """
            SELECT sp_rayon.id AS sp_id,
                   (sp_reserve.qty_stock + sp_reserve.qty_ug) AS available_qty
            FROM store_inventory_line sil
            JOIN stock_produit sp_rayon
              ON sp_rayon.produit_id = sil.produit_id
             AND sp_rayon.storage_id = sil.storage_id
            JOIN storage s_reserve
              ON s_reserve.magasin_id = :magasinId
             AND s_reserve.storage_type = 'SAFETY_STOCK'
            JOIN stock_produit sp_reserve
              ON sp_reserve.produit_id = sil.produit_id
             AND sp_reserve.storage_id = s_reserve.id
            WHERE sil.store_inventory_id = :inventoryId
              AND (sp_rayon.qty_stock + sp_rayon.qty_ug) < COALESCE(sp_rayon.seuil_mini, 0)
              AND (sp_reserve.qty_stock + sp_reserve.qty_ug) > 0
            """;

    /**
     * Produits dont la réserve (SAFETY_STOCK) est à 0 ET qui ont du stock en rayon (storage
     * inventorié) &gt; 0. → Suggestion : réapprovisionner la réserve depuis le rayon.
     */
    private static final String SQL_RESERVE_FROM_RAYON =
        """
            SELECT sp_reserve.id AS sp_id,
                   (sp_rayon.qty_stock + sp_rayon.qty_ug) AS available_qty
            FROM store_inventory_line sil
            JOIN stock_produit sp_rayon
              ON sp_rayon.produit_id = sil.produit_id
             AND sp_rayon.storage_id = sil.storage_id
            JOIN storage s_reserve
              ON s_reserve.magasin_id = :magasinId
             AND s_reserve.storage_type = 'SAFETY_STOCK'
            JOIN stock_produit sp_reserve
              ON sp_reserve.produit_id = sil.produit_id
             AND sp_reserve.storage_id = s_reserve.id
            WHERE sil.store_inventory_id = :inventoryId
              AND (sp_reserve.qty_stock + sp_reserve.qty_ug) = 0
              AND (sp_rayon.qty_stock + sp_rayon.qty_ug) > 0
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
