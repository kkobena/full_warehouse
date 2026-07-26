package com.kobe.warehouse.service.dto.produit.merge;

/**
 * Choix de l'utilisateur pour résoudre une collision de lot (même {@code num_lot})
 * détectée entre un produit source et le produit cible lors d'une fusion de produits.
 */
public enum LotConflictAction {
    /** Additionne les quantités du lot source dans le lot cible, puis supprime le lot source. */
    MERGE,
    /** Supprime le lot source sans reporter sa quantité sur le lot cible. */
    DELETE,
}
