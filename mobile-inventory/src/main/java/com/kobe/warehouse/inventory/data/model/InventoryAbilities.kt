package com.kobe.warehouse.inventory.data.model

/**
 * Codes des permissions ACTION utilisées par l'app d'inventaire.
 * Fournis par GET /api/nav/my-abilities (liste plate de codes exécutables).
 */
object InventoryAbilities {
    // La clôture (pr-cloture-inventaire) n'est pas exposée ici : elle se fait
    // exclusivement depuis le poste (web / Tauri).
    const val VIEW_STOCK = "pr-voir-stock-inventaire"
}
