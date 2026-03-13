package com.kobe.warehouse.service.stock;

import java.util.Map;
import java.util.Set;

/**
 * Service de calcul de stock pour l'inventaire.
 * Remplace getStock() de InventaireServiceImpl avec :
 *  - protection NPE (retourne 0 si stockProduit absent)
 *  - chargement en masse (1 requête pour N produits, élimine le N+1)
 *
 * Distinction rayon / réserve :
 *  Un produit peut avoir deux StockProduit pour le même magasin
 *  (un pour le storage rayon, un pour le storage réserve).
 *  - buildStockMapByStorage → filtre par storage précis (RAYON, STORAGE)
 *  - buildStockMapByMagasin → agrège TOUS les storages du magasin (MAGASIN)
 */
public interface InventoryStockService {

    /**
     * Pré-charge le stock pour un storage précis (rayon ou réserve, pas les deux).
     * Usage : inventaires de type RAYON ou STORAGE.
     */
    Map<Integer, Integer> buildStockMapByStorage(Integer storageId, Set<Integer> produitIds);

    /**
     * Pré-charge le stock agrégé sur TOUS les storages d'un magasin (rayon + réserve).
     * Usage : inventaires de type MAGASIN, FAMILLY, PERIME, ALERTE_PEREMPTION, VENDU, INVENDU.
     */
    Map<Integer, Integer> buildStockMapByMagasin(Integer magasinId, Set<Integer> produitIds);

    /**
     * Retourne le stock d'un seul produit dans un storage précis.
     * Retourne 0 si introuvable. Ne lève jamais de NPE.
     */
    int getStockByStorage(Integer storageId, Integer produitId);

    /**
     * Retourne le stock total (rayon + réserve) d'un produit pour un magasin.
     * Retourne 0 si introuvable. Ne lève jamais de NPE.
     */
    int getStockByMagasin(Integer magasinId, Integer produitId);
}
