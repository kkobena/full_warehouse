package com.kobe.warehouse.domain.enumeration;

/** Onglet du pilotage exportable dans le rapport PDF ; {@code code} est celui du {@code nav_item} qui porte le droit d'export. */
public enum OngletRapportPilotage {
    TABLEAU_DE_BORD("pilotage.tableau-de-bord", "Tableau de bord", "Comment va l'officine ?"),
    ANALYSER("pilotage.analyser", "Analyser", "Pourquoi ça bouge ?"),
    COMPARER_ANNEES("pilotage.comparer-annees", "Comparer les années", "Où en est-on par rapport aux années passées ?"),
    RENTABILITE_REMISES("pilotage.rentabilite-remises", "Rentabilité & remises", "Où est-ce que je gagne ou perds de l'argent ?"),
    ACHATS_STOCK("pilotage.achats-stock", "Achats & stock", "Mes achats et mon stock sont-ils maîtrisés ?"),
    TRESORERIE_TIERS_PAYANT("pilotage.tresorerie-tiers-payant", "Trésorerie & tiers payant", "Mon argent rentre-t-il ?"),
    CLIENTS_EQUIPE("pilotage.clients-equipe", "Clients & équipe", "Qui vient, qui vend ?");

    private final String code;
    private final String libelle;
    private final String question;

    OngletRapportPilotage(String code, String libelle, String question) {
        this.code = code;
        this.libelle = libelle;
        this.question = question;
    }

    public String getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getQuestion() {
        return question;
    }
}
