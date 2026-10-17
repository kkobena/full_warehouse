package com.kobe.warehouse.domain.enumeration;

/**
 * Origine d'un code alternatif : elle dit à qui imputer une liaison erronée.
 */
public enum CodeAliasSource {
    /** Saisi ou confirmé par un utilisateur, à la réception ou depuis la fiche produit. */
    MANUAL,
    /** Déduit par le système sans confirmation. Aucun écrit de ce type à ce jour. */
    AUTO_LEARNED,
    /** Reprise d'un fichier fournisseur ou d'un catalogue externe. */
    IMPORT,
}
