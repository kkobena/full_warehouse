package com.kobe.warehouse.service.dto.pilotage.rapport;

import java.util.List;

/**
 * Ligne d'un tableau du rapport : une cellule par colonne, valeur et, s'il y a une référence, variation.
 *
 * @param total ligne de total, en gras
 */
public record LigneRapportDTO(String libelle, List<CelluleRapportDTO> cellules, boolean total) {
    public LigneRapportDTO(String libelle, List<CelluleRapportDTO> cellules) {
        this(libelle, cellules, false);
    }

    /** @param texte cellule de texte (date, vendeur…), alignée à gauche */
    public record CelluleRapportDTO(String valeur, VariationRapportDTO variation, boolean texte) {
        public CelluleRapportDTO(String valeur, VariationRapportDTO variation) {
            this(valeur, variation, false);
        }

        public static CelluleRapportDTO lireTexte(String valeur) {
            return new CelluleRapportDTO(valeur == null ? "—" : valeur, null, true);
        }
    }
}
