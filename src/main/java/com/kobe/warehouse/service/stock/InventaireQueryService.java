package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.InventoryExportWrapper;
import com.kobe.warehouse.service.dto.StoreInventoryGroupExport;
import com.kobe.warehouse.service.dto.StoreInventoryLotGroupExport;
import com.kobe.warehouse.service.dto.filter.StoreInventoryExportRecord;
import com.kobe.warehouse.service.dto.filter.StoreInventoryLineFilterRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryLineRecord;
import java.util.List;
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

    /**
     * Construit le wrapper d'export (groupes + totaux) pour un inventaire.
     * Retourne {@code null} si aucun article trouvé.
     */
    InventoryExportWrapper exportInventory(StoreInventoryExportRecord inventoryExportRecord);

    /**
     * Retourne les groupes d'articles ventilés pour l'export.
     */
    List<StoreInventoryGroupExport> getStoreInventoryToExport(StoreInventoryExportRecord filterRecord);

    /**
     * Retourne les groupes de lots pour l'export PDF en mode gestion de lot.
     */
    List<StoreInventoryLotGroupExport> getLotGroupsForExport(Long inventoryId);
}
