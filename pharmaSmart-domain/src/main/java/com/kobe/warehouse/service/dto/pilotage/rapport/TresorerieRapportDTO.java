package com.kobe.warehouse.service.dto.pilotage.rapport;

import java.util.List;

/**
 * Onglet « Trésorerie & tiers payant » tel que l'imprime le rapport : encaissements, tiers payant, différés. Graphes en data URI,
 * {@code null} sans donnée ; {@code caissiers} est {@code null} sans le droit « Clients & équipe », comme à l'écran.
 *
 * @param noteEcheancier comment l'échéance de chaque facture est estimée
 */
public record TresorerieRapportDTO(
    List<TuileRapportDTO> tuilesEncaissements,
    String titreEncaissements,
    String encaissementsParMode,
    TableauRapportDTO modes,
    TableauRapportDTO caissiers,
    List<TuileRapportDTO> tuilesTiersPayant,
    String titreCourbePartTiersPayant,
    String courbePartTiersPayant,
    String noteEcheancier,
    String echeancier,
    String vieillissementTiersPayant,
    TableauRapportDTO organismes,
    List<TuileRapportDTO> tuilesDifferes,
    String vieillissementDifferes,
    TableauRapportDTO clientsDebiteurs
) {}
