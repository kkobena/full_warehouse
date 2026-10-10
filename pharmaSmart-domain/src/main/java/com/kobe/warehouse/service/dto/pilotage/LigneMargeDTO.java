package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/**
 * Marge d'un élément (famille, laboratoire, produit). L'écart de marge se décompose exactement en effet volume (CA HT en plus ou
 * en moins, au taux de référence) et effet taux (au nouveau CA HT).
 *
 * @param ecartTaux variation du taux de marge, en points
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record LigneMargeDTO(
    String cle,
    String libelle,
    long caHt,
    long coutHt,
    long marge,
    Double taux,
    Double tauxReference,
    Double ecartTaux,
    Long effetVolume,
    Long effetTaux
) {}
