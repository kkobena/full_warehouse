package com.kobe.warehouse.service.dto;

/**
 * Produit retrouvé à partir d'un code scanné.
 *
 * @param produitId   identifiant du produit
 * @param libelle     libellé du produit, pour l'affichage en cas de désambiguïsation
 * @param matchedCode code qui a permis la correspondance
 * @param matchedBy   source de la correspondance
 */
public record ProductResolveResult(Integer produitId, String libelle, String matchedCode, MatchedBy matchedBy) {
    public enum MatchedBy {
        /** CIP de la fiche fournisseur. */
        CODE_CIP,
        /** EAN de la fiche fournisseur. */
        CODE_EAN,
        /** EAN laboratoire porté par le produit. */
        CODE_EAN_LABO,
        /** Code alternatif mémorisé dans {@code produit_code_alias}. */
        ALIAS,
    }
}
