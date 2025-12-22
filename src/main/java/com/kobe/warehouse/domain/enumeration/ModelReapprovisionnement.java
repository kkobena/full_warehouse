package com.kobe.warehouse.domain.enumeration;

/**
 * Énumération des modèles de calcul de réapprovisionnement.
 */
public enum ModelReapprovisionnement {
    /**
     * Modèle classique basé sur les ventes des 3 derniers mois avec moyenne simple
     */
    CLASSIQUE,

    /**
     * Modèle SEMOIS (Stock Économique Mensuel d'Objectif Interne de Sécurité)
     * Basé sur VMM pondéré, marge de sécurité et classe de criticité
     */
    SEMOIS
}
