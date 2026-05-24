package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.domain.enumeration.StorageType;

/**
 * Événement publié après la clôture d'un inventaire (commit effectué). Consommé de manière
 * asynchrone pour générer des suggestions post-inventaire.
 */
public record InventoryClosedEvent(
    Long storeInventoryId,
    InventoryCategory inventoryCategory,
    StorageType storageType,
    Integer storageId,
    Integer magasinId,
    Integer userId
) {

}
