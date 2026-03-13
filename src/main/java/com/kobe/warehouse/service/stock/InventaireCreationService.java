package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.records.StoreInventoryRecord;

/**
 * Service de création et d'initialisation d'un inventaire.
 * Remplace create() de InventaireServiceImpl avec :
 *  - requêtes INSERT paramétrées (named parameters) au lieu de String.format() avec IDs
 *  - pas de concaténation SQL → supprime le risque d'injection et améliore les plans d'exécution
 */
public interface InventaireCreationService {

    /**
     * Crée un inventaire et insère les lignes correspondantes selon la catégorie
     * (MAGASIN, RAYON, STORAGE, FAMILLY) en utilisant des paramètres nommés.
     */
    StoreInventoryDTO create(StoreInventoryRecord record);
}
