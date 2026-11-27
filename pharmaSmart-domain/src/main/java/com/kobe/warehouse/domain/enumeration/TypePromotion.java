package com.kobe.warehouse.domain.enumeration;

/** Mécanique d'une promotion. */
public enum TypePromotion {
    /** {@code valeur} est un taux entier en pourcentage. */
    PROMO_POURCENTAGE,
    /** {@code valeur} est un montant unitaire en francs, déduit de chaque unité remisée. */
    PROMO_MONTANT,
    /** Le prix unitaire promotionnel est porté par chaque produit ({@code prix_promo}). */
    PROMO_PRIX_FIXE,
    /**
     * Offre N+M : {@code quantite_min} porte N, les unités achetées, et {@code valeur} M, les unités
     * offertes — les moins chères de chaque groupe, tous articles de la promotion confondus.
     */
    PROMO_OFFRE_NM,
    /**
     * Pack : plusieurs articles différents vendus ensemble. {@code valeur} porte le prix du pack, et
     * chaque article sa quantité dans le pack ({@code promotion_produit.quantite}).
     */
    PROMO_PACK,
    /**
     * Remise dès un montant d'achat : {@code valeur} porte le taux, {@code seuil_montant} le montant
     * des articles de la promotion qui la déclenche. Atteint, le taux vaut pour tout ce montant.
     */
    PROMO_SEUIL_MONTANT
}
