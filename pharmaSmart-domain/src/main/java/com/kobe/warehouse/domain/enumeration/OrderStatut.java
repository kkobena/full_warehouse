package com.kobe.warehouse.domain.enumeration;

/**
 * The OrderStatut enumeration.
 */
public enum OrderStatut {
    REQUESTED,
    RECEIVED,
    CLOSED,
    /** Commande originale conservée après clonage lors d'une finalisation hors-date. */
    ARCHIVED,
}
