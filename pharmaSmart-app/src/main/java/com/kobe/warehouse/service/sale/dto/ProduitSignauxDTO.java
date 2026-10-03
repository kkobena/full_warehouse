package com.kobe.warehouse.service.sale.dto;

/** Ce que le comptoir doit savoir d'un produit du panier : statut légal, générique, péremption. */
public record ProduitSignauxDTO(
    Integer produitId,
    String statutLegal,
    String typeGenerique,
    /** Lot en stock le plus proche de sa péremption, s'il est périmé ou proche de l'être ; nul sinon. */
    String peremptionLot,
    /** Date de ce lot (AAAA-MM-JJ). */
    String peremptionDate,
    /** Au-delà de cette date (AAAA-MM-JJ), un lot n'est plus « proche de sa péremption » : seuil de configuration. */
    String dateLimitePeremption,
    /** Stock restant (rayon + réserve, UG comprises) : celui que mesure l'alerte « ALERTE » du tableau de bord. */
    int stockRestant,
    /** Seuil mini du produit ; 0 : pas de seuil, aucune alerte de stock faible. */
    int seuilMini
) {}
