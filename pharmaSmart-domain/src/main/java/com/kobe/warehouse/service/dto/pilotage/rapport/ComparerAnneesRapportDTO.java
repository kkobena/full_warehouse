package com.kobe.warehouse.service.dto.pilotage.rapport;

/**
 * Onglet « Comparer les années » tel que l'imprime le rapport. Graphes en data URI ; {@code saisonnalite} est {@code null} sans
 * année close ou sur un indicateur non additif, {@code familles} et {@code natures} s'il ne se ventile pas ainsi.
 *
 * @param reglage le réglage de l'écran, dit en clair (indicateur, nombre d'années, lecture des mois, filtre)
 */
public record ComparerAnneesRapportDTO(
    String reglage,
    String lecture,
    String titreCourbes,
    String courbes,
    String trimestres,
    String saisonnalite,
    TableauRapportDTO moisAnnees,
    TableauRapportDTO synthese,
    TableauRapportDTO familles,
    TableauRapportDTO natures
) {}
