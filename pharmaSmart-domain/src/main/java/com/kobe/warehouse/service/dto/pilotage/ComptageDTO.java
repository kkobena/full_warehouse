package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Nombre, quantité et montant d'un élément (fournisseur, produit…) ; projection commune aux ruptures et ventes manquées. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ComptageDTO(String cle, String libelle, Long nombre, Long quantite, Long montant) {}
