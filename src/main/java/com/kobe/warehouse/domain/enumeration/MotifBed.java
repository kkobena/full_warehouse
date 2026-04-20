package com.kobe.warehouse.domain.enumeration;

public enum MotifBed {
    RETOUR_CLIENT("Retour client"),
    ECHANTILLON("Echantillon / Don laboratoire"),
    TRANSFERT_ENTRANT("Transfert entrant inter-pharmacie"),
    REGULARISATION("Régularisation positive"),
    CORRECTION_ERREUR("Correction d'erreur"),
    BASCULEMENT("Basculement depuis autre logiciel"),
    BASCULEMENT_PRESTIGE("Basculement depuis Prestige"),
    AUTRE("Autre");

    private final String label;

    MotifBed(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
