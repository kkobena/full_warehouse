package com.kobe.warehouse.service.dto.pilotage.rapport;

import java.util.List;

/**
 * Onglet « Rentabilité & remises » tel que l'imprime le rapport : marge, remises, démarque. Courbes en data URI, {@code null}
 * sans donnée ; {@code vendeurs} est {@code null} sans le droit « Clients & équipe », comme à l'écran.
 *
 * @param lectureMix « Le taux de marge recule de 0,6 pt : … », {@code null} sans référence
 */
public record RentabiliteRapportDTO(
    List<TuileRapportDTO> tuilesMarge,
    String lectureMix,
    String titreCourbeMarge,
    String courbeTauxMarge,
    String titreFaiblesMarges,
    TableauRapportDTO faiblesMarges,
    TableauRapportDTO margeParFamille,
    TableauRapportDTO ventesAMargeNegative,
    List<TuileRapportDTO> tuilesRemises,
    String titreCourbeRemises,
    String courbeRemises,
    TableauRapportDTO octrois,
    TableauRapportDTO tranches,
    TableauRapportDTO vendeurs,
    TableauRapportDTO plusFortesRemises,
    List<TuileRapportDTO> tuilesDemarque,
    TableauRapportDTO demarqueParMotif,
    TableauRapportDTO produitsDemarques
) {}
