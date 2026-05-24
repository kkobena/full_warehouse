package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.records.InventoryProgressRecord;

/**
 * Service de suivi de la progression d'un inventaire en cours.
 * Fournit les indicateurs clés sans recharger toute la grille.
 */
public interface InventaireProgressService {

    /**
     * Retourne les indicateurs de progression pour un inventaire :
     * - total de lignes
     * - lignes déjà saisies
     * - lignes avec écart
     * - pourcentage de complétion
     */
    InventoryProgressRecord getProgress(Long inventoryId);
}
