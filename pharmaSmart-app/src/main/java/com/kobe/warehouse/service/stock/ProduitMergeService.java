package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.produit.merge.ProduitMergePreviewDTO;
import com.kobe.warehouse.service.dto.produit.merge.ProduitMergeRequestDTO;
import com.kobe.warehouse.service.dto.produit.merge.ProduitMergeResultDTO;
import java.util.List;

/**
 * Fusionne un ou plusieurs produits en doublon (source) dans un produit à conserver (cible) :
 * migre les stocks, lots, prix, fournisseurs, rayons, ventes et données annexes, puis désactive
 * les produits source. Aucune suppression physique.
 */
public interface ProduitMergeService {
    /** Simule la fusion sans rien modifier ; remonte les conflits (notamment de lots) à trancher. */
    ProduitMergePreviewDTO preview(Integer targetId, List<Integer> sourceIds);

    /** Exécute la fusion. Rejette si des conflits de lot signalés par {@link #preview} ne sont pas résolus. */
    ProduitMergeResultDTO merge(ProduitMergeRequestDTO request);
}
