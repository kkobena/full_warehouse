package com.kobe.warehouse.service.declaration_ca.dto;

import java.time.LocalDate;

/**
 * Critères de consultation d'un journal d'exclusion.
 *
 * <p>Les trois journaux partagent ces critères, y compris ceux qui ne les emploient pas tous :
 * {@code tiersPayantId} n'a de sens que pour les ventes tiers-payant. Un paramètre inerte coûte
 * moins qu'un troisième objet de critères à maintenir en parallèle.
 *
 * @param recherche fragment de code ou de libellé produit ; {@code null} ou vide = pas de filtre
 * @param tiersPayantId restreint aux ventes relevant de ce tiers-payant
 */
public record JournalExclusionParamDTO(
    LocalDate dateDebut,
    LocalDate dateFin,
    String recherche,
    Integer tiersPayantId
) {
    public JournalExclusionParamDTO {
        if (dateDebut != null && dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date de début.");
        }
    }
}
