package com.kobe.warehouse.domain.enumeration;

/** Codes `nav_item` du pilotage (migration V2.1.32) : la page et ses onglets, qui portent les droits. */
public enum DroitPilotage {
    PAGE("pilotage"),
    TABLEAU_DE_BORD("pilotage.tableau-de-bord"),
    ANALYSER("pilotage.analyser"),
    COMPARER_ANNEES("pilotage.comparer-annees"),
    RENTABILITE_REMISES("pilotage.rentabilite-remises"),
    ACHATS_STOCK("pilotage.achats-stock"),
    TRESORERIE_TIERS_PAYANT("pilotage.tresorerie-tiers-payant"),
    CLIENTS_EQUIPE("pilotage.clients-equipe"),
    OBJECTIFS("pilotage.objectifs");

    private final String code;

    DroitPilotage(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
