package com.kobe.warehouse.service.dto.pilotage.rapport;

import java.util.List;

/**
 * Onglet « Clients & équipe » tel que l'imprime le rapport : fréquentation, clients identifiés, équipe.
 *
 * @param lectureCreneaux « Créneaux les plus chargés : mardi 10 h (42 ventes), … », {@code null} sans vente
 * @param frequentation heures × jours de la semaine, {@code null} sans vente
 * @param enBaisse {@code null} sans client en baisse, comme l'écran le masque
 * @param vendeursParHeure CA par vendeur et par heure, {@code null} sans vente
 */
public record ClientsEquipeRapportDTO(
    String lectureCreneaux,
    CarteChaleurRapportDTO frequentation,
    String ventesParJour,
    List<TuileRapportDTO> tuilesClients,
    TableauRapportDTO enBaisse,
    TableauRapportDTO vendeurs,
    TableauRapportDTO vendeursParHeure
) {}
