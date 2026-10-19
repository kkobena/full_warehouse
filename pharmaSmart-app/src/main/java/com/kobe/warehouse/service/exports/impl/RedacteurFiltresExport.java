package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.service.dto.exports.OptionFiltreDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dit en une ligne par filtre sur quoi un export a porté, pour l'historique. Les listes fermées (type de vente, type de
 * mouvement) se lisent en clair tant qu'elles restent courtes ; un référentiel ne se compte, car en nommer les valeurs
 * demanderait de le relire à chaque ligne de l'historique.
 */
final class RedacteurFiltresExport {

    /** Au-delà, on compte plutôt que d'allonger la ligne : l'historique et l'en-tête du PDF restent lisibles. */
    private static final int VALEURS_NOMMEES_MAX = 3;

    private RedacteurFiltresExport() {}

    static List<String> rediger(Map<FiltreExport, List<String>> filtres) {
        List<String> lignes = new ArrayList<>();
        for (FiltreExport filtre : FiltreExport.values()) {
            List<String> valeurs = filtres.get(filtre);
            if (valeurs == null || valeurs.isEmpty()) {
                continue;
            }
            boolean nommable = !filtre.getSource().estReferentiel() && valeurs.size() <= VALEURS_NOMMEES_MAX;
            lignes.add(filtre.getLibelle() + " : " + (nommable ? nommer(filtre, valeurs) : compter(valeurs)));
        }
        return lignes;
    }

    private static String compter(List<String> valeurs) {
        return valeurs.size() == 1 ? "1 valeur" : valeurs.size() + " valeurs";
    }

    private static String nommer(FiltreExport filtre, List<String> valeurs) {
        List<String> libelles = new ArrayList<>();
        for (String valeur : valeurs) {
            libelles.add(lireLibelle(filtre, valeur));
        }
        return String.join(", ", libelles);
    }

    private static String lireLibelle(FiltreExport filtre, String valeur) {
        for (OptionFiltreDTO option : filtre.getSource().getOptions()) {
            if (option.valeur().equals(valeur)) {
                return option.libelle();
            }
        }
        return valeur;
    }
}
