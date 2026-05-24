package com.kobe.warehouse.service.dto.dashboard;

import java.util.List;

/**
 * DTO principal du dashboard préparateur en pharmacie.
 * Synthèse croisée Caisse + Ventes + Différés + Commandes.
 */
public record CaissierDashboardDTO(
    CaisseStatusDTO caisseStatus,
    SessionEncaissementsDTO sessionEncaissements,
    ResumeDifferesDTO resumeDifferes,
    List<LivraisonAttendueDTO> livraisonsAttendues,
    List<VenteRecenteDTO> ventesRecentes
) {}
