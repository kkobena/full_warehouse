package com.kobe.warehouse.domain.enumeration;

/**
 * Classification Pareto ABC étendue — 5 classes alignées avec {@link ClasseCriticite}.
 * Source : colonne {@code classe_pareto} de {@code v_abc_pareto_analysis}.
 */
public enum ClassePareto {
    /** Produits représentant les premiers ~60 % du CA cumulé — gestion maximale */
    A_PLUS,
    /** Produits entre ~60 % et ~80 % du CA cumulé — gestion prioritaire */
    A,
    /** Produits entre ~80 % et ~95 % du CA cumulé — gestion normale */
    B,
    /** Produits entre ~95 % et ~99 % du CA cumulé — gestion allégée */
    C,
    /** Produits au-delà de 99 % ou sans ventes — gestion minimale */
    D
}
