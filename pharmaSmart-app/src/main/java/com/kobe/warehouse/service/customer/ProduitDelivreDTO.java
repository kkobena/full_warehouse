package com.kobe.warehouse.service.customer;

import java.time.LocalDateTime;

/** Un produit délivré à un client, agrégé sur une période : « le même que la dernière fois ». */
public record ProduitDelivreDTO(
    Integer produitId,
    String libelle,
    Long nombreDelivrances,
    Long quantite,
    Long montant,
    LocalDateTime derniereDelivrance
) {}
