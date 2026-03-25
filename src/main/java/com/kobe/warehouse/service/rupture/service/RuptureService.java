package com.kobe.warehouse.service.rupture.service;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.Produit;

public interface RuptureService {
    void createRupture(Produit produit, Fournisseur fournisseur, int qty);

    /**
     * Marque toutes les ruptures ouvertes d'un produit comme résolues
     * (product_still_out_of_stock = false) lors de la réception d'un bon de livraison.
     *
     * @param produit le produit qui rentre en stock
     */
    void markProductAsBackInStock(Produit produit);
}
