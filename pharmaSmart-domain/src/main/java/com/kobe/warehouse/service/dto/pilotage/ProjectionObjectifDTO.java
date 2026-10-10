package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/**
 * Projection de fin du mois en cours.
 *
 * @param methode comment la projection est faite, dite en clair
 * @param atteinteProjetee projection / objectif (%)
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ProjectionObjectifDTO(Double realiseADate, Double projection, Double objectif, Double atteinteProjetee, Boolean tenu, String methode) {}
