package com.kobe.warehouse.service.dto.records;

import java.time.LocalDate;

public record StoreInventoryRecord(
    Long id,
    Integer storage,
    Integer rayon,
    String inventoryCategory,
    Integer famillyId,
    String description,
    // ── Paramètres optionnels pour les nouveaux types ─────────────────────────
    /** VENDU / INVENDU : début de période */
    LocalDate dateFrom,
    /** VENDU / INVENDU : fin de période */
    LocalDate dateTo,
    /** ALERTE_PEREMPTION : nb de jours avant péremption (défaut 90) */
    Integer alerteJours
) {

    /** Constructeur de compatibilité pour le code existant (sans les nouveaux champs) */
    public StoreInventoryRecord(
        Long id,
        Integer storage,
        Integer rayon,
        String inventoryCategory,
        Integer famillyId,
        String description
    ) {
        this(id, storage, rayon, inventoryCategory, famillyId, description, null, null, null);
    }
}
