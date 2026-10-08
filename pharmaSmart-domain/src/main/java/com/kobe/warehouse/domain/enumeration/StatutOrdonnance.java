package com.kobe.warehouse.domain.enumeration;

/** Statut d'une ordonnance, dérivé à la lecture (jamais stocké). Seule {@code EN_COURS} se propose à la reprise. */
public enum StatutOrdonnance {
    EN_COURS,
    TERMINEE,
    EXPIREE,
}
