package com.kobe.warehouse.service.reassort.dto;

import com.kobe.warehouse.service.errors.GenericError;

/**
 * DTO pour une demande de répartition manuelle.
 *
 * @param stockSourceId          identifiant du StockProduit source
 * @param stockDestinationId     identifiant du StockProduit destination (null si createNewDestination=true)
 * @param quantity               quantité à transférer (> 0)
 * @param seuilMini              seuil minimal optionnel
 * @param createNewDestination   si true, crée automatiquement un emplacement SAFETY_STOCK pour le produit
 */
public record RepartionQueryDto(
    Integer stockSourceId,
    Integer stockDestinationId,
    int quantity,
    Integer seuilMini,
    boolean createNewDestination
) {
    public RepartionQueryDto {
        if (quantity <= 0) {
            throw new GenericError("La quantité doit être supérieure à 0");
        }
        if (!createNewDestination && stockDestinationId == null) {
            throw new GenericError("L'identifiant du stock destination est requis");
        }
    }
}
