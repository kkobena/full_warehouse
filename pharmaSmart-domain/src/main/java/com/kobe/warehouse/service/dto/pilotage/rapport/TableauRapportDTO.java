package com.kobe.warehouse.service.dto.pilotage.rapport;

import java.util.List;

/**
 * Tableau du rapport, prêt à imprimer : {@code entete} est le coin (« Période », « Famille / Année »…), une colonne par
 * libellé de {@code colonnes}.
 *
 * @param mention phrase sous le tableau (« … et 12 autres »), {@code null} sinon
 */
public record TableauRapportDTO(String entete, List<String> colonnes, List<LigneRapportDTO> lignes, String mention) {
    public TableauRapportDTO(String entete, List<String> colonnes, List<LigneRapportDTO> lignes) {
        this(entete, colonnes, lignes, null);
    }
}
