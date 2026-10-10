package com.kobe.warehouse.domain.enumeration;

/** Lecture d'un mois dans « Comparer les années » : le mois seul, le cumul depuis janvier, ou les douze mois qui s'y terminent. */
public enum ModeAnnees {
    MENSUEL,
    CUMULE,
    GLISSANT,
}
