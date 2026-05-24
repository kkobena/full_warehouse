package com.kobe.warehouse.domain.enumeration;

/**
 * Mode de rangement automatique à la réception d'une commande.
 *
 * <ul>
 *   <li>{@link #MANUAL} (défaut) : les suggestions rayon→réserve sont créées à la réception ;
 *       un modal de confirmation est présenté au pharmacien lors de la validation finale.
 *       Il peut accepter (transfert immédiat) ou refuser (suggestion reste ouverte).</li>
 *   <li>{@link #AUTO} : transfert implicite sans confirmation, exécuté automatiquement
 *       à la validation finale.</li>
 *   <li>{@link #ALL_RAYON} : aucune suggestion ni transfert — tout le stock reste en rayon.</li>
 * </ul>
 */
public enum PutawayMode {
    MANUAL,
    AUTO,
    ALL_RAYON
}
