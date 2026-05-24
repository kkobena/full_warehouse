package com.kobe.warehouse.service.reglement.dto;

public enum ModeEditionReglement {
    /**
     * Reglement total  facture groupée
     */
    GROUPE_TOTAL,
    /**
     * Reglement total  facture individuelle
     */
    FACTURE_TOTAL,
    /**
     * Reglement partiel facture individuelle
     */
    FACTURE_PARTIEL,
    /**
     * Reglement partiel facture groupée
     */
    GROUPE_PARTIEL,
    /**
     * Reglement partiel de toutes les  factures selectionnées
     */
    FACTURE_PARTIEL_ALL,
    /**
     * Reglement partiel de toutes les  factures groupées selectionnées
     */
    GROUPE_PARTIEL_ALL,
}
