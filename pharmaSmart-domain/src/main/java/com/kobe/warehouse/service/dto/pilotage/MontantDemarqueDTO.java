package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Sorties de stock d'un motif ou d'un produit, valorisées au prix d'achat courant. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record MontantDemarqueDTO(String cle, String libelle, Long quantite, Long valeur) {}
