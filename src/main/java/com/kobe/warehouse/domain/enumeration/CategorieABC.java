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
}
