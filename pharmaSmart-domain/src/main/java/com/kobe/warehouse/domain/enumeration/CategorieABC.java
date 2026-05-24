package com.kobe.warehouse.domain.enumeration;

public enum CategorieABC {
    A("Produits à forte rotation", 1.96),
    B("Produits à rotation moyenne", 1.65),
    C("Produits à faible rotation", 1.28);

    private final String value;
    private final double z; //Le score Z est une mesure statistique qui indique combien d'écarts-types une valeur est éloignée de la moyenne. Il est utilisé pour évaluer la position d'une valeur par rapport à une distribution normale.

    public String getValue() {
        return value;
    }

    CategorieABC(String value, double z) {
        this.value = value;
        this.z = z;
    }

    public double getZ() {
        return z;
    }

    /**
     * Convertit CategorieABC vers ClasseCriticite avec mapping intelligent.
     * Par défaut: A → A, B → B, C → C
     * Peut être surchargé par configuration produit.
     *
     * @return La classe de criticité équivalente
     */
    public ClasseCriticite toClasseCriticite() {
        return switch (this) {
            case A -> ClasseCriticite.A;
            case B -> ClasseCriticite.B;
            case C -> ClasseCriticite.C;
        };
    }

    /**
     * Convertit ClasseCriticite vers CategorieABC (perte d'information possible).
     * Mapping: A+ → A, A → A, B → B, C → C, D → C
     *
     * @param classeCriticite La classe de criticité à convertir
     * @return La catégorie ABC équivalente
     */
    public static CategorieABC fromClasseCriticite(ClasseCriticite classeCriticite) {
        if (classeCriticite == null) {
            return C; // Par défaut
        }

        return switch (classeCriticite) {
            case A_PLUS, A -> A;
            case B -> B;
            case C, D -> C;
        };
    }
}
