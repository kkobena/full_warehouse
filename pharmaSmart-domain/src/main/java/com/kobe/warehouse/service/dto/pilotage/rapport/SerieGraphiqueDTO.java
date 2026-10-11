package com.kobe.warehouse.service.dto.pilotage.rapport;

import java.util.List;

/**
 * Une série d'un graphe du rapport. Réalisé, référence (pointillés gris) et objectif (tirets) ont leur couleur ; une série
 * {@code PALETTE} prend la couleur de son rang (une année, un mode d'encaissement…), en trait plein ou pointillé ;
 * {@code FANTOME} est l'accent pâli, la référence à côté des barres classées de l'onglet Analyser.
 */
public record SerieGraphiqueDTO(String libelle, List<Double> valeurs, StyleSerie style) {
    public enum StyleSerie {
        REALISE,
        REFERENCE,
        OBJECTIF,
        PALETTE,
        PALETTE_POINTILLE,
        FANTOME,
    }
}
