package com.kobe.warehouse.service.declaration_ca.dto;

/**
 * Où en est une ponction dans son cycle de vie.
 *
 * <p>Il n'existe pas de statut « verrouillée ». Un verrou posé à la main supposerait que quelqu'un
 * pense à le poser, au bon moment, après chaque déclaration — et une ponction oubliée resterait
 * annulable indéfiniment. L'irréversibilité vient donc du <strong>temps</strong> : passé le délai
 * configuré, une ponction validée n'est plus annulable, sans qu'aucun geste n'ait été nécessaire.
 */
public enum StatutPonction {
    /** Calculée mais non appliquée : n'occupe pas la période et ne modifie aucune vente. */
    SIMULATION,
    /** Appliquée : les montants déclarables des ventes sont réduits, la période est réservée. */
    VALIDEE,
    /** Défaite : les montants d'origine sont rétablis et la période redevient disponible. */
    ANNULEE,
}
