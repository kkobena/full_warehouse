package com.kobe.warehouse.domain.enumeration;

/**
 * Pourquoi le montant déclarable d'une ligne est inférieur à son montant réel.
 *
 * <p>Deux rôles, dont le second n'est pas documentaire : au-delà de la traçabilité, ce motif sert de
 * <strong>critère d'éligibilité à la ponction</strong>. Une vente dont une ligne porte déjà un motif
 * n'est pas ponctionnable — sans quoi le plafond par vente n'aurait plus de lecture unique, et la
 * répartition sur les règlements devrait se combiner avec une réduction déjà répartie.
 */
public enum ExclusionMotif {
    /** Le produit appartient à un rayon exclu du chiffre d'affaires à déclarer. */
    RAYON,
    /** La vente relève d'un tiers-payant exclu. */
    TIERS_PAYANT,
    /** Les unités gratuites de la ligne sont retirées. */
    UG,
    /** La ligne a été réduite par une ponction de période. */
    PONCTION,
    /** Retraitement saisi à la main, hors des mécanismes automatiques. */
    MANUEL,
}
