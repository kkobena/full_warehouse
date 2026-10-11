package com.kobe.warehouse.service.dto.pilotage.rapport;

import java.util.List;

/**
 * Carte de chaleur imprimée en tableau : chaque case porte sa valeur en chiffres, la couleur n'est jamais seule à porter
 * l'information ; le fond, calculé côté serveur, est d'autant plus soutenu que la valeur est forte.
 */
public record CarteChaleurRapportDTO(String entete, List<String> colonnes, List<LigneChaleurDTO> lignes) {
    public record LigneChaleurDTO(String libelle, List<CaseChaleurDTO> cases) {}

    /** @param fond couleur CSS ({@code #rrggbb}) */
    public record CaseChaleurDTO(String valeur, String fond) {}
}
