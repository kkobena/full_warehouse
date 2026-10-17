package com.kobe.warehouse.domain.enumeration;

/**
 * Usage de l'étiquette : ce n'est pas une affaire de format, mais de contenu.
 *
 * <p>Le format dit où l'étiquette tombe sur la planche ou le rouleau ; le type dit ce qui y est
 * écrit, et c'est lui qui choisit le gabarit. Une réglette de gondole ne porte pas de lot, une
 * étiquette de posologie ne porte pas de prix, une préparation magistrale porte une composition
 * que rien d'autre ne porte.
 */
public enum TypeEtiquette {
    /** Code du catalogue en Code 39, libellé et prix : l'étiquette posée sur la boîte. */
    STANDARD("Standard"),

    /** La même, plus le lot, la péremption et un GS1 DataMatrix. */
    LOT("Lot (DataMatrix)"),

    /** Réglette de gondole : collée sur l'étagère, le prix en gros et le code en petit. */
    REGLETTE("Réglette de gondole"),

    /** Posologie remise au patient : prescripteur, produit, prise et durée. Jamais de prix. */
    POSOLOGIE("Posologie"),

    /** Préparation magistrale : composition, dates et prescripteur, saisis à l'impression. */
    PREPARATION("Préparation magistrale");

    private final String libelle;

    TypeEtiquette(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Les trois usages servis par le gabarit commun, par opposition aux étiquettes d'article. */
    public boolean isUsage() {
        return this == REGLETTE || this == POSOLOGIE || this == PREPARATION;
    }

    /** Un prix sur une ordonnance ou sur un pot de préparation n'aurait rien à y faire. */
    public boolean isAvecPrix() {
        return this == STANDARD || this == LOT || this == REGLETTE;
    }
}
