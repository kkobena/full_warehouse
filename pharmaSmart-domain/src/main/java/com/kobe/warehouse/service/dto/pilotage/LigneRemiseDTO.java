package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/**
 * Remises d'un élément (mode d'octroi, tranche, vendeur). Les taux varient en points.
 *
 * @param tauxMarge vide quand la ventilation ne connaît pas la marge (en-têtes)
 * @param alerte vendeur dont le taux de remise dépasse le multiple fixé du taux de l'équipe
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record LigneRemiseDTO(
    String cle,
    String libelle,
    CelluleAnalyseDTO nbVentes,
    CelluleAnalyseDTO caTtc,
    CelluleAnalyseDTO remises,
    CelluleAnalyseDTO tauxRemise,
    CelluleAnalyseDTO tauxMarge,
    boolean alerte
) {}
