package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.filter.StoreInventoryLineFilterRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryLineRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service de consultation paginée des lignes d'inventaire avec correction du N+1.
 * Fournit une implémentation unifiée de getAllByInventory / getInventoryItems
 * en pré-chargeant les stocks en masse via InventoryStockService.buildStockMap().
 */
public interface InventaireQueryService {

    /**
     * Retourne une page de lignes d'inventaire.
     *
     * @param filter          critères de filtre (inventoryId, rayon, storage, search, selectedFilter)
     * @param pageable        pagination
     * @param excludeIfClosed si true, retourne une page vide pour un inventaire CLOSED
     */
    Page<StoreInventoryLineRecord> getInventoryPage(
        StoreInventoryLineFilterRecord filter,
        Pageable pageable,
        boolean excludeIfClosed
    );
}
