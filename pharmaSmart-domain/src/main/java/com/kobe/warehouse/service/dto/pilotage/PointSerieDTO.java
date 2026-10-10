package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/**
 * Valeur d'un indicateur sur une tranche de la période, et sur la tranche de même rang de la référence, avec l'écart.
 *
 * @param objectif objectif de la tranche (prorata des jours pour un mois entamé) ; {@code null} sans objectif ou sans le droit
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PointSerieDTO(LocalDate debut, LocalDate fin, String libelle, Double valeur, Double valeurReference, Double ecart, Double ecartPct, Double objectif) {}
