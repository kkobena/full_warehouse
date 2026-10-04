package com.kobe.warehouse.service.stock.dto;

/**
 * Un produit proposé à la place d'un autre au comptoir : le produit au format de la recherche (donc directement ajoutable au
 * panier), et la nature du lien — {@code GENERIQUE} ou {@code THERAPEUTIQUE}, ce dernier relevant d'un avis du pharmacien.
 */
public record SubstitutPropose(ProduitSearch produit, String typeSubstitut, String typeSubstitutLibelle) {}
