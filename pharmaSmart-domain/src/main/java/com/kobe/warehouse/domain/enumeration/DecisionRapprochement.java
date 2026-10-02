package com.kobe.warehouse.domain.enumeration;

/**
 * Suite donnée à une proposition. Seule EN_ATTENTE est recalculée : AUTO, VALIDE et REJETE sont
 * définitives (REJETE vient aussi de la dissociation d'une DCI sur la fiche produit).
 */
public enum DecisionRapprochement {
    EN_ATTENTE,
    /** Acceptée par règle (statut SUR ou PAR_DCI), sans relecture. */
    AUTO,
    VALIDE,
    REJETE,
}
