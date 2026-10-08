package com.kobe.warehouse.service.dto.controle;

/** Produit du panier sans molécule exploitable : « non contrôlé » n'est pas « sans alerte ». */
public record ProduitNonControleDTO(Integer produitId, String libelle) {}
