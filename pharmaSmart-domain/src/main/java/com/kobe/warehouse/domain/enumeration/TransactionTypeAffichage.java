package com.kobe.warehouse.domain.enumeration;

/**
 * Libellé d'un type de mouvement de caisse, en deux longueurs.
 *
 * <p>Le libellé long nomme précisément l'opération : c'est celui d'un ticket ou d'un détail,
 * où la place ne manque pas et où l'ambiguïté coûte cher.
 *
 * <p>Le COURT est fait pour la liste des mouvements, dont la colonne « Type » est étroite :
 * « Règlement facture fournisseur » y déborde et écrase les colonnes voisines. Il dit la même
 * chose en deux mots — le lecteur a la référence, la date et le montant sous les yeux, il n'a
 * pas besoin qu'on lui répète « facture ».
 */
public enum TransactionTypeAffichage {
    VNO("VNO", "VNO"),
    VO("VO", "VO"),
    VENTES_DEPOTS("Ventes dépôts", "Dépôts"),
    ENTREE_CAISSE("Entrée de caisse", "Entrée"),
    SORTIE_CAISSE("Sortie de caisse", "Sortie"),
    REGLEMENT_DIFFERE("Règlements différés", "Rgt différé"),
    REGLEMENT_TIERS_PAYANT("Règlements tiers payant", "Rgt tiers payant"),
    REGLEMENT_FOURNISSEUR("Règlement facture fournisseur", "Rgt fournisseur"),
    DEPOT_CAUTION("Dépôt de caution", "Caution");

    private final String value;
    private final String valueCourt;

    TransactionTypeAffichage(String value, String valueCourt) {
        this.value = value;
        this.valueCourt = valueCourt;
    }

    public String getValue() {
        return value;
    }

    /** Libellé abrégé, pour les colonnes étroites. */
    public String getValueCourt() {
        return valueCourt;
    }
}
