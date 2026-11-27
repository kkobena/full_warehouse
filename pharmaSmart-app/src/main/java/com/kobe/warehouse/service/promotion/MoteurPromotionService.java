package com.kobe.warehouse.service.promotion;

import com.kobe.warehouse.domain.Sales;

/**
 * Applique les promotions à une vente. Une fonction du panier, pas un geste : elle est rappelée à
 * chaque mutation, et elle est idempotente.
 *
 * <p>Ordre d'appel, dans le recalcul de la vente : {@link #effacer} avant le calcul des montants,
 * puis la remise de grille éventuelle, puis {@link #appliquer}. Une ligne déjà remisée par un humain
 * (grille ou remise posée) n'est jamais touchée.
 */
public interface MoteurPromotionService {
    /** Retire de la vente tout ce que le moteur y a posé, et rien d'autre. */
    void effacer(Sales vente);

    /**
     * Pose sur chaque ligne non remisée la promotion la plus avantageuse pour le client. Vrai si au
     * moins une ligne a reçu une promotion : les totaux de remise de la vente sont alors à refaire.
     */
    boolean appliquer(Sales vente);
}
