package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.FiltreExport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lecture des réglages reçus du catalogue, ramenés au type qu'attend le service de l'édition. Un réglage absent reste nul :
 * c'est ainsi que la requête, ou le service, prend son propre défaut — celui de l'écran d'origine.
 */
final class ReglagesRapports {

    private ReglagesRapports() {}

    /** Un réglage à valeur unique ; nul pour laisser le défaut s'appliquer. */
    static <T> T lireUnique(Map<FiltreExport, List<String>> filtres, FiltreExport filtre, Class<T> type) {
        List<String> valeurs = filtres.get(filtre);
        return valeurs == null || valeurs.isEmpty() ? null : type.cast(filtre.getSource().convertir(valeurs.getFirst()));
    }

    static <T> List<T> lireListe(Map<FiltreExport, List<String>> filtres, FiltreExport filtre, Class<T> type) {
        List<String> valeurs = filtres.get(filtre);
        if (valeurs == null) {
            return List.of();
        }
        List<T> lues = new ArrayList<>(valeurs.size());
        for (String valeur : valeurs) {
            lues.add(type.cast(filtre.getSource().convertir(valeur)));
        }
        return lues;
    }

    static Integer lireEntier(Map<FiltreExport, List<String>> filtres, FiltreExport filtre) {
        return lireUnique(filtres, filtre, Integer.class);
    }

    static String lireTexte(Map<FiltreExport, List<String>> filtres, FiltreExport filtre) {
        return lireUnique(filtres, filtre, String.class);
    }
}
