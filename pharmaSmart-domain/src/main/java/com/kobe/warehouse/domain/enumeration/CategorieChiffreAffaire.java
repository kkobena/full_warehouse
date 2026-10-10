package com.kobe.warehouse.domain.enumeration;

import java.util.EnumSet;
import java.util.Set;

public enum CategorieChiffreAffaire {
    CA,
    CA_DEPOT,
    CALLEBASE,
    TO_IGNORE,
    IMPORT;

    /** CA de l'officine : ventes au dépôt (CA_DEPOT) exclues, ventes importées du dépôt comprises (elles portent CA). */
    public static Set<CategorieChiffreAffaire> officine() {
        return EnumSet.of(CA);
    }
}
