package com.kobe.warehouse.domain.enumeration;

/**
 * Nature d'un magasin.
 *
 * <p>Les trois valeurs ne se distinguent pas par leur autonomie d'exploitation — un dépôt comme
 * une unité de gestion tiennent leur propre stock — mais par leur existence juridique. Seule
 * l'officine certifie : elle porte la licence et l'identité FNE, et tout ce qui est facturé part
 * sous la sienne.
 */
public enum TypeMagasin {
    /** L'établissement principal : racine de l'arbre, porteur de la licence et de l'identité FNE. */
    OFFICINE("Officine"),
    /**
     * Établissement rattaché, dans une localité dépourvue d'officine. Sa marchandise lui est
     * remise en consignation et il ne commande pas lui-même.
     */
    DEPOT("Dépôt extension"),
    /**
     * Unité interne au même lieu physique — une parapharmacie par exemple. Elle tient son stock,
     * sa caisse et ses commandes, mais n'a aucune existence juridique propre : elle facture sous
     * l'identité de son officine.
     */
    UNITE_GESTION("Unité de gestion");

    private final String libelle;

    TypeMagasin(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /**
     * Cet établissement a-t-il une existence juridique propre — licence, pharmacien responsable ?
     *
     * <p>À ne pas confondre avec la certification : même un dépôt, qui est un établissement
     * distinct, ne porte pas d'identité FNE dans ce logiciel, puisqu'il ne vend pas au comptoir et
     * que ses reventes remontent par import. Pour la FNE, c'est la racine qui tranche.
     */
    public boolean estEtablissementDistinct() {
        return this != UNITE_GESTION;
    }
}
