package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.FiltresExportConverter;
import com.kobe.warehouse.domain.enumeration.SujetExport;
import com.kobe.warehouse.service.errors.GenericError;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Ramène les filtres d'une demande à ce que l'entrée du catalogue annonce : un filtre venu d'ailleurs ne doit ni filtrer ni
 * rester dans l'historique, et un réglage à valeur unique n'en garde qu'une. Les valeurs sont vérifiées ici, pendant que
 * l'utilisateur attend sa réponse, plutôt que de faire échouer la génération en tâche de fond sur un message illisible.
 */
final class FiltresDemandes {

    /** Longueur de la colonne {@code filtres} ; au-delà, l'insertion échouerait sur un message illisible. */
    private static final int LONGUEUR_MAX = 1_000;

    private FiltresDemandes() {}

    static Map<FiltreExport, List<String>> retenir(SujetExport export, Map<FiltreExport, List<String>> filtres) {
        Map<FiltreExport, List<String>> retenus = new EnumMap<>(FiltreExport.class);
        if (filtres == null || filtres.isEmpty()) {
            return retenus;
        }
        for (FiltreExport filtre : export.getFiltres()) {
            List<String> valeurs = filtres.get(filtre);
            if (valeurs == null || valeurs.isEmpty()) {
                continue;
            }
            for (String valeur : valeurs) {
                verifier(filtre, valeur);
            }
            retenus.put(filtre, filtre.estMultiple() ? List.copyOf(valeurs) : List.of(valeurs.getFirst()));
        }
        verifierTaille(retenus);
        return retenus;
    }

    private static void verifierTaille(Map<FiltreExport, List<String>> retenus) {
        String colonne = new FiltresExportConverter().convertToDatabaseColumn(retenus);
        if (colonne != null && colonne.length() > LONGUEUR_MAX) {
            throw new GenericError("Trop de valeurs retenues pour être gardées avec l'export : affinez votre choix");
        }
    }

    private static void verifier(FiltreExport filtre, String valeur) {
        if (valeur == null || valeur.isBlank()) {
            throw new GenericError("Valeur vide pour le filtre « " + filtre.getLibelle() + " »");
        }
        try {
            filtre.getSource().convertir(valeur);
        } catch (RuntimeException e) {
            throw new GenericError("Valeur inattendue pour le filtre « " + filtre.getLibelle() + " » : " + valeur);
        }
    }
}
