package com.kobe.warehouse.service.dto.records;

import java.time.LocalDate;

/**
 * Ligne de la grille lot. {@code id} nul signale une ligne sans lot : le produit est dans le
 * périmètre de l'inventaire mais n'a aucun {@code inventory_lot} (pas de lot, ou lots tous à
 * zéro). Elle se compte alors au niveau de la ligne produit, d'où la présence de {@code version}
 * — le verrou optimiste attendu par l'API ligne.
 */
public record StoreInventoryLotLineRecord(
    Long id,
    Long storeInventoryLineId,
    int produitId,
    String produitCip,
    String produitLibelle,
    String numLot,
    LocalDate expiryDate,
    Integer quantityOnHand,
    Integer quantityInit,
    Integer gap,
    boolean updated,
    String classePareto,
    Long version
) {
    /**
     * Rejoue la ligne avec une autre quantité initiale. Les lignes sans lot n'ont pas de stock
     * initial propre : il leur est apporté après coup par le service, depuis la même source que
     * la vue produit.
     */
    public StoreInventoryLotLineRecord withQuantityInit(Integer value) {
        return new StoreInventoryLotLineRecord(
            id, storeInventoryLineId, produitId, produitCip, produitLibelle, numLot, expiryDate,
            quantityOnHand, value, gap, updated, classePareto, version
        );
    }
}
