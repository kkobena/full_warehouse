package com.kobe.warehouse.domain.enumeration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Le catalogue entier et la lecture d'un code reçu de l'écran. Les deux énumérations ne partagent aucun nom de constante : un
 * code désigne donc une entrée et une seule.
 */
public final class SujetsExport {

    /**
     * Les éditions les plus demandées, en tête du catalogue et dans cet ordre : statistiques de TVA, tableaux de bord,
     * récapitulatif de caisse, rapport d'activité, puis le reste du pilotage.
     *
     * <p>C'est le seul endroit à modifier pour changer l'ordre d'apparition — les deux énumérations gardent, elles, l'ordre qui
     * fait sens à la lecture du code.
     */
    private static final List<SujetExport> VEDETTES = List.of(
        RapportExport.RAPPORT_TVA,
        RapportExport.DECLARATION_TVA,
        RapportExport.TABLEAU_DE_BORD_CA,
        RapportExport.PILOTAGE_TABLEAU_DE_BORD,
        RapportExport.RECAPITULATIF_CAISSE,
        RapportExport.RAPPORT_ACTIVITE,
        RapportExport.PILOTAGE_ANALYSER,
        RapportExport.PILOTAGE_ANNEES,
        RapportExport.EXPORT_COMPTABLE,
        RapportExport.BALANCE_CAISSE,
        RapportExport.TABLEAU_PHARMACIEN
    );

    private static final List<SujetExport> TOUS = rassembler();
    private static final Map<String, SujetExport> PAR_CODE = indexer();

    private SujetsExport() {}

    /** Les plus demandées d'abord, puis les exports de données, puis les autres éditions : l'ordre dans lequel l'écran les range. */
    public static List<SujetExport> lister() {
        return TOUS;
    }

    /** @throws IllegalArgumentException si le code ne désigne aucune entrée du catalogue */
    public static SujetExport lire(String code) {
        SujetExport sujet = code == null ? null : PAR_CODE.get(code);
        if (sujet == null) {
            throw new IllegalArgumentException("Export inconnu : " + code);
        }
        return sujet;
    }

    private static List<SujetExport> rassembler() {
        // Un ensemble ordonné : une vedette n'apparaît pas deux fois, et nul besoin de la retirer de sa liste d'origine.
        Set<SujetExport> sujets = new LinkedHashSet<>(VEDETTES);
        sujets.addAll(List.of(ExportDonnees.values()));
        sujets.addAll(List.of(RapportExport.values()));
        return List.copyOf(new ArrayList<>(sujets));
    }

    private static Map<String, SujetExport> indexer() {
        Map<String, SujetExport> parCode = new HashMap<>();
        for (SujetExport sujet : TOUS) {
            parCode.put(sujet.name(), sujet);
        }
        return Map.copyOf(parCode);
    }
}
