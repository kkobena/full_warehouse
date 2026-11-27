package com.kobe.warehouse.service.dto.promotion;

/**
 * Un article du périmètre, avec son prix de référence et le prix qu'il aura en promotion pour une
 * unité — de quoi relire la promotion avant de la valider.
 */
public record PromotionProduitDTO(
        Integer produitId,
        String libelle,
        String codeCip,
        Integer prixReference,
        Integer prixPromo,
        Integer prixApresPromo,
        /** Unités dans le pack ; 1 pour les autres mécaniques. */
        int quantite,
        /** Médicament à prix administré (TVA 0) : signalé, pas refusé — c'est au pharmacien d'en décider. */
        boolean prixAdministre,
        /** Vendu en promotion au prix d'achat ou en dessous (un pack se juge en entier) : signalé, pas refusé. */
        boolean aPerte) {

    public PromotionProduitDTO(
            Integer produitId,
            String libelle,
            String codeCip,
            Integer prixReference,
            Integer prixPromo,
            Integer prixApresPromo,
            int quantite) {
        this(produitId, libelle, codeCip, prixReference, prixPromo, prixApresPromo, quantite, false, false);
    }

    public PromotionProduitDTO(
            Integer produitId,
            String libelle,
            String codeCip,
            Integer prixReference,
            Integer prixPromo,
            Integer prixApresPromo) {
        this(produitId, libelle, codeCip, prixReference, prixPromo, prixApresPromo, 1);
    }
}
