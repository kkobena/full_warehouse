package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/**
 * Synthèse d'une année ; la marge reste vide sans le droit « Rentabilité & remises », le nombre de ventes sur un périmètre
 * filtré par famille (les lignes ne comptent pas les ventes).
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record SyntheseAnneeDTO(
    int annee,
    boolean complete,
    Double caTtc,
    Double margeBrute,
    Double tauxMarge,
    Double nbVentes,
    Double panierMoyen,
    Double remises,
    Double croissanceCa
) {}
