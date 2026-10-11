package com.kobe.warehouse.service.dto.pilotage.rapport;

import java.util.List;

/**
 * Onglet « Achats & stock » tel que l'imprime le rapport : achats, achats / ventes, stock, ruptures et péremptions. Courbes en
 * data URI, {@code null} sans donnée ; un tableau de ruptures vide est {@code null}, comme l'écran le masque.
 *
 * @param noteStock « Fin octobre 2026 ; valeur au prix d'achat… »
 */
public record AchatsStockRapportDTO(
    List<TuileRapportDTO> tuilesAchats,
    String titreCourbeAchats,
    String courbeAchats,
    TableauRapportDTO achatsParFamille,
    TableauRapportDTO achatsParFournisseur,
    List<TuileRapportDTO> tuilesAchatsVentes,
    String titreCourbeAchatsVentes,
    String courbeAchatsVentes,
    TableauRapportDTO achatsVentesParFamille,
    List<TuileRapportDTO> tuilesStock,
    String noteStock,
    String courbeStock,
    String titreDormants,
    TableauRapportDTO dormants,
    TableauRapportDTO stockParFamille,
    List<TuileRapportDTO> tuilesRuptures,
    TableauRapportDTO rupturesParFournisseur,
    TableauRapportDTO rupturesParProduit,
    TableauRapportDTO ventesManquees,
    TableauRapportDTO peremptions
) {}
