package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/**
 * Achats d'un fournisseur ou d'une famille. Délai et conformité ne valent que pour un fournisseur.
 *
 * @param part part des achats de la période (%)
 * @param conformite quantités reçues / quantités commandées (%)
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record LigneAchatsDTO(String cle, String libelle, CelluleAnalyseDTO montant, Double part, Long nbBons, Double delaiMoyen, Double conformite) {}
