package com.kobe.warehouse.domain.enumeration;

/**
 * Comment une remise a été accordée au comptoir : par le vendeur qui détient le privilège {@code PR_AJOUTER_REMISE_VENTE}, ou
 * après qu'un détenteur a saisi sa clé de sécurité (ligne de {@code utilisation_cle_securite} sur la vente).
 */
public enum OctroiRemise {
    AUCUNE,
    PRIVILEGE,
    AUTORISEE,
}
