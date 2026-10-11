package com.kobe.warehouse.service.dto.pilotage.rapport;

/**
 * Onglet Analyser tel que l'imprime le rapport, selon l'affichage de l'écran : un graphe (barres classées ou courbes, en data
 * URI) ou un tableau croisé, puis le détail par élément.
 *
 * @param reglage indicateurs, ventilations, nombre d'éléments et tri, dits en clair
 * @param chemin fil de la descente (« Famille : Antalgiques › Produit : … »), {@code null} sans descente
 * @param noteIgnores indicateurs que l'axe ne sait pas ventiler, {@code null} sinon
 * @param graphe {@code null} pour un affichage en tableau ou croisé
 * @param croise {@code null} hors affichage croisé ; vide sans second axe
 */
public record AnalyserRapportDTO(
    String reglage,
    String chemin,
    String noteIgnores,
    String titreGraphe,
    String graphe,
    TableauRapportDTO croise,
    String titreDetail,
    TableauRapportDTO detail
) {}
