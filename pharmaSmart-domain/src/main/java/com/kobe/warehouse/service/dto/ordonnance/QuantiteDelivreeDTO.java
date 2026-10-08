package com.kobe.warehouse.service.dto.ordonnance;

/** Quantité délivrée d'une ligne d'ordonnance : résultat d'agrégation du dépôt. */
public record QuantiteDelivreeDTO(Integer ligneId, Long quantite) {}
