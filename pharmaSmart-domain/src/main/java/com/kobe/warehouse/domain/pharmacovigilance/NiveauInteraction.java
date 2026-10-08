package com.kobe.warehouse.domain.pharmacovigilance;

/** Niveaux du thésaurus des interactions, du plus grave au moins grave. */
public enum NiveauInteraction {
    /** Contre-indication : prise en compte explicite, motif exigé. */
    CI,
    /** Association déconseillée. */
    AD,
    /** Précaution d'emploi. */
    PE,
    /** À prendre en compte. */
    APEC,
}
