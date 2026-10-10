package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/**
 * Objectif face au réalisé, sur un mois ou un cumul.
 *
 * @param enCours mois en cours : le réalisé n'est qu'à date
 * @param atteinte réalisé / objectif (%)
 * @param tenu objectif tenu (atteint, ou plafond respecté pour un indicateur à faire baisser) ; {@code null} sans objectif ou sans réalisé
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record SuiviMoisDTO(int mois, Double objectif, Double realise, Double ecart, Double atteinte, Boolean tenu, boolean enCours) {}
