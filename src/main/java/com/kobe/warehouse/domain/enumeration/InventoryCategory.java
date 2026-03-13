package com.kobe.warehouse.domain.enumeration;

public enum InventoryCategory {
    // ── Types de périmètre ────────────────────────────────────────────────────
    /** Tout le magasin (tous produits actifs) */
    MAGASIN,
    /** Un storage (point de vente) */
    STORAGE,
    /** Un rayon */
    RAYON,
    /** Une famille de produits */
    FAMILLY,

    // ── Types thématiques (nouveaux) ─────────────────────────────────────────
    /** Produits avec lots périmés et stock > 0 — obligation réglementaire */
    PERIME,
    /** Produits dont les lots expirent dans X jours — anticipation retours */
    ALERTE_PEREMPTION,
    /** Produits vendus sur une période donnée — vérification cohérence stock/ventes */
    VENDU,
    /** Produits sans aucune vente sur une période — stock dormant / déstockage */
    INVENDU,
    /** Produits dont le stock est inférieur ou égal au seuil minimum */
    SOUS_SEUIL,
    /** Produits en rupture totale (qty_stock = 0) */
    EN_RUPTURE,
}
