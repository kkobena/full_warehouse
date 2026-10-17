package com.kobe.warehouse.domain.enumeration;

/**
 * Nature d'un code alternatif mémorisé pour un produit.
 */
public enum CodeAliasType {
    /** GTIN-14 du fabricant, contenu de l'AI 01 d'un DataMatrix. */
    GTIN_14,
    EAN_13,
    EAN_8,
    CIP_13,
    CIP_7,
    /** Code propre à un grossiste ou à l'officine, hors nomenclature connue. */
    CUSTOM,
}
