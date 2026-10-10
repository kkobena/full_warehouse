package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Lots qui périment dans un mois : quantité restante et sa valeur au prix d'achat du lot (projection). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PeremptionMoisDTO(Integer annee, Integer mois, Long quantite, Long valeur) {}
