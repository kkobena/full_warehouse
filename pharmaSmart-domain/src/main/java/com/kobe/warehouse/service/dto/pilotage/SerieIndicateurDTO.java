package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Un indicateur sur la période et sa référence. {@code ecart} est en valeur (en points pour un pourcentage), {@code ecartPct}
 * en % de la référence ({@code null} si la référence est nulle).
 *
 * @param objectif objectif de la période (prorata des jours d'un mois entamé), {@code null} sans objectif ou sans le droit
 * @param projection fin du mois en cours, seulement quand la période est ce mois à date et l'indicateur un montant
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record SerieIndicateurDTO(
    IndicateurPilotageDTO indicateur,
    Double valeur,
    Double valeurReference,
    Double ecart,
    Double ecartPct,
    List<PointSerieDTO> points,
    Double objectif,
    ProjectionObjectifDTO projection
) {}
