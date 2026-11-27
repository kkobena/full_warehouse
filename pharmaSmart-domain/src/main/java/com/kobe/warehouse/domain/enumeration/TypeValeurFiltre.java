package com.kobe.warehouse.domain.enumeration;

/** Type de la valeur d'un filtre d'export, une fois ramenée du texte reçu de l'écran à ce qu'attend la requête. */
public enum TypeValeurFiltre {
    ENTIER,
    /** Identifiant sur 64 bits, comme celui d'une promotion : la requête refuse un {@code Integer} à sa place. */
    ENTIER_LONG,
    TEXTE,
    ENUM,
}
