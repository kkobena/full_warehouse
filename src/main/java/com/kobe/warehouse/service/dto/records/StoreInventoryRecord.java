package com.kobe.warehouse.service.dto.records;

import java.time.LocalDate;
import java.util.List;

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
    Integer alerteJours,
    /** ABC : classe Pareto à filtrer — 'A', 'B' ou 'C' ; null = toutes */
    String classePareto,
    /**
     * Optionnel — userId à utiliser à la place de l'utilisateur authentifié courant.
     * Utilisé par le scheduler (pas de contexte de sécurité).
     */
    Integer userId,
    /**
     * SELECTION_PRODUIT : liste explicite d'IDs de produits à inventorier.
     * Obligatoire quand inventoryCategory = SELECTION_PRODUIT.
     */
    List<Integer> produitIds
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
        this(id, storage, rayon, inventoryCategory, famillyId, description, null, null, null, null, null, null);
    }

    /** Constructeur sans userId (API REST) */
    public StoreInventoryRecord(
        Long id,
        Integer storage,
        Integer rayon,
        String inventoryCategory,
        Integer famillyId,
        String description,
        LocalDate dateFrom,
        LocalDate dateTo,
        Integer alerteJours,
        String classePareto
    ) {
        this(id, storage, rayon, inventoryCategory, famillyId, description, dateFrom, dateTo, alerteJours, classePareto, null, null);
    }
}
