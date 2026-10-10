package com.kobe.warehouse.domain.enumeration;

/** Période de référence d'une comparaison du pilotage. */
public enum TypeComparaison {
    AUCUNE,
    PERIODE_PRECEDENTE,
    MEME_PERIODE_N_1,
    ANNEE_N_MOINS_K,
    PERSONNALISEE,
    /** Les objectifs mensuels : seules les séries (Tableau de bord) s'y comparent ; les autres onglets restent sans référence. */
    OBJECTIF,
}
