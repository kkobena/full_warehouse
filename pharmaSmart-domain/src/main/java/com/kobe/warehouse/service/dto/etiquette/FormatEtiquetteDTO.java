package com.kobe.warehouse.service.dto.etiquette;

import com.kobe.warehouse.domain.enumeration.FormatEtiquette;

/**
 * Un format d'étiquette, tel que l'écran d'impression le présente.
 *
 * <p>Le front n'embarque pas la liste : il la lit, pour qu'un format ajouté côté serveur apparaisse
 * sans redéploiement du bundle. {@code capacite} lui sert à borner la case de départ, et
 * {@code support} à savoir s'il doit seulement la masquer — un rouleau n'a pas de planche entamée.
 * {@code supportLibelle} groupe la liste déroulante.
 */
public record FormatEtiquetteDTO(
    String code,
    String libelle,
    String reference,
    String support,
    String supportLibelle,
    double largeurMm,
    double hauteurMm,
    int colonnes,
    int rangees,
    int capacite,
    boolean parDefaut
) {
    public static FormatEtiquetteDTO depuis(FormatEtiquette format) {
        return new FormatEtiquetteDTO(
            format.name(),
            format.getLibelle(),
            format.getReference(),
            format.getSupport().name(),
            format.getSupport().getLibelle(),
            format.getLargeurMm(),
            format.getHauteurMm(),
            format.getColonnes(),
            format.getRangees(),
            format.getCapacite(),
            format == FormatEtiquette.parDefaut()
        );
    }
}
