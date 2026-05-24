package com.kobe.warehouse.domain.enumeration;

/**
 * Classification de criticité des produits pour la méthode SEMOIS.
 * Détermine le coefficient de sécurité appliqué au stock objectif.
 */
public enum ClasseCriticite {
    /**
     * Produits vitaux - Priorité maximale
     * Exemples: Insulines, anticoagulants, antibiotiques critiques
     * Coefficient de sécurité: 1.5 (stock de sécurité élevé)
     */
    A_PLUS("Produits vitaux", 1.5),

    /**
     * Produits à forte rotation - Haute importance
     * Exemples: Paracétamol, AINS courants, produits OTC populaires
     * Coefficient de sécurité: 1.0 (stock de sécurité standard)
     */
    A("Forte rotation", 1.0),

    /**
     * Produits à rotation moyenne - Importance modérée
     * Exemples: Vitamines, compléments alimentaires
     * Coefficient de sécurité: 0.7 (stock de sécurité réduit)
     */
    B("Rotation moyenne", 0.7),

    /**
     * Produits à faible rotation - Faible importance
     * Exemples: Produits de niche, spécialités rares
     * Coefficient de sécurité: 0.4 (stock de sécurité minimal)
     */
    C("Faible rotation", 0.4),

    /**
     * Produits à très faible rotation - Importance minimale
     * Exemples: Produits obsolescents, en fin de vie
     * Coefficient de sécurité: 0.2 (stock de sécurité très faible)
     */
    D("Très faible rotation", 0.2);

    private final String description;
    private final double coefficientDefaut;

    ClasseCriticite(String description, double coefficientDefaut) {
        this.description = description;
        this.coefficientDefaut = coefficientDefaut;
    }

    /**
     * @return Description lisible de la classe de criticité
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return Coefficient de sécurité par défaut pour cette classe
     */
    public double getCoefficientDefaut() {
        return coefficientDefaut;
    }

    /**
     * Obtenir la classe de criticité depuis une chaîne (ex: "A+", "A", "B")
     *
     * @param value La valeur textuelle ("A+", "A", "B", "C", "D")
     * @return La classe de criticité correspondante
     * @throws IllegalArgumentException si la valeur n'est pas reconnue
     */
    public static ClasseCriticite fromString(String value) {
        if (value == null) {
            return B; // Par défaut
        }

        return switch (value.toUpperCase()) {
            case "A+", "A_PLUS", "APLUS" -> A_PLUS;
            case "A" -> A;
            case "B" -> B;
            case "C" -> C;
            case "D" -> D;
            default -> throw new IllegalArgumentException("Classe de criticité inconnue: " + value);
        };
    }

    /**
     * @return Code court pour la base de données ("A+", "A", "B", "C", "D")
     */
    public String getCode() {
        return switch (this) {
            case A_PLUS -> "A+";
            case A -> "A";
            case B -> "B";
            case C -> "C";
            case D -> "D";
        };
    }
}
