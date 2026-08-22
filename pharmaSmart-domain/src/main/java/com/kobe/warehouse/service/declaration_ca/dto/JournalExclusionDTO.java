package com.kobe.warehouse.service.declaration_ca.dto;

import java.util.List;

/**
 * Le contenu d'un journal d'exclusion : les indicateurs, puis les lignes.
 *
 * <p>Un seul aller-retour pour les deux — l'écran n'a jamais d'usage de l'un sans l'autre, et deux
 * appels distincts ouvriraient la porte à un bandeau et un tableau calculés sur des instants
 * différents.
 *
 * @param ventes renseigné pour le journal tiers-payant, qui se lit par vente ; vide sinon
 * @param lignes renseigné pour les journaux unités gratuites et rayon ; vide pour le tiers-payant,
 *     dont les lignes se chargent vente par vente
 * @param tronque {@code true} si le plafond de lignes a été atteint — les indicateurs, eux, portent
 *     bien sur la totalité de la période
 */
public record JournalExclusionDTO(
    JournalKpiDTO kpi,
    List<JournalVenteDTO> ventes,
    List<JournalLigneDTO> lignes,
    boolean tronque
) {}
