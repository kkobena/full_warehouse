package com.kobe.warehouse.domain.enumeration;

/**
 * La décision humaine sur une promotion. L'état « à venir / en cours / terminée » ne se stocke pas : il
 * se lit sur les dates, comparées à la date de la vente.
 */
public enum StatutPromotion {
    /** En préparation : jamais appliquée. */
    BROUILLON,
    /** Appliquée par le moteur entre ses dates. */
    VALIDEE,
    /** Validée puis arrêtée à la main ; réactivable. */
    SUSPENDUE,
    /** Définitive : une promotion annulée ne redevient jamais validée. */
    ANNULEE
}
