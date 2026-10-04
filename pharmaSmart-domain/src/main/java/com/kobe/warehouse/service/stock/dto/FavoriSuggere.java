package com.kobe.warehouse.service.stock.dto;

/**
 * Un produit à épingler à la grille du comptoir, proposé d'après les ventes : le produit au format de la recherche, et ce qui
 * justifie la suggestion sur les 30 derniers jours — nombre de ventes (tickets) distinctes, puis quantité vendue.
 */
public record FavoriSuggere(ProduitSearch produit, long nbVentes, long qteVendue) {}
