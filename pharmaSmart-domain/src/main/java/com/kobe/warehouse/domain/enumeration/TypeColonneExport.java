package com.kobe.warehouse.domain.enumeration;

/** Type d'une colonne d'export : il décide du format de la cellule (nombre, date) en CSV comme en XLSX. */
public enum TypeColonneExport {
    TEXTE,
    ENTIER,
    MONTANT,
    DECIMAL,
    DATE,
    DATE_HEURE,
}
