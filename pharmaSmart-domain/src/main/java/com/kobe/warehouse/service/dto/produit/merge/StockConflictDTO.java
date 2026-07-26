package com.kobe.warehouse.service.dto.produit.merge;

/**
 * Conflit détecté entre un {@code StockProduit} du produit source et un {@code StockProduit}
 * du produit cible partageant le même {@code storage} (contrainte unique
 * {@code (storage_id, produit_id)}). Contrairement aux lots, ce conflit n'est jamais fusionné
 * automatiquement : la ligne source est réaffectée telle quelle est impossible (collision), la
 * ligne cible reste inchangée, et la quantité du doublon n'est PAS reportée automatiquement sur
 * la cible — un ajustement manuel de stock est nécessaire côté utilisateur pour refléter le
 * stock physique réellement disponible.
 */
public record StockConflictDTO(
    Integer storageId,
    String storageLibelle,
    Integer sourceQtyStock,
    Integer sourceQtyVirtual,
    Integer sourceQtyUG,
    Integer targetQtyStock,
    Integer targetQtyVirtual,
    Integer targetQtyUG
) {}
