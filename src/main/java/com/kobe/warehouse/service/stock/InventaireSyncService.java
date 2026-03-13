package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import com.kobe.warehouse.service.dto.records.BatchSyncResultRecord;
import java.util.List;

/**
 * Service de synchronisation batch des lignes d'inventaire.
 * Remplace synchronizeStoreInventoryLine() de InventaireServiceImpl :
 *  - chargement en masse via findAllById (1 requête au lieu de N)
 *  - sauvegarde via saveAll (batch SQL au lieu de N saveAndFlush)
 */
public interface InventaireSyncService {

    /**
     * Synchronise une liste de lignes d'inventaire en mode batch.
     * Retourne un résumé : lignes sauvegardées, lignes en erreur, IDs en erreur.
     */
    BatchSyncResultRecord synchronize(List<StoreInventoryLineDTO> dtos);
}
