package com.kobe.warehouse.service.dto.promotion;

import com.kobe.warehouse.domain.enumeration.StatutPromotion;
import com.kobe.warehouse.domain.enumeration.TypePromotion;
import java.time.LocalDate;
import java.util.List;

public record PromotionDTO(
        Long id,
        String code,
        String libelle,
        TypePromotion type,
        Integer valeur,
        LocalDate dateDebut,
        LocalDate dateFin,
        StatutPromotion statut,
        EtatPromotion etat,
        int priorite,
        int quantiteMin,
        Integer quantiteMaxParVente,
        Integer quantiteMaxTotale,
        /** Unités déjà remisées sur les ventes clôturées et non annulées. */
        long unitesRemisees,
        int nombreProduits,
        List<PromotionProduitDTO> produits,
        /** Code-barres d'un pack, imprimé sur son emballage ; nul pour les autres mécaniques. */
        String codeBarrePack,
        /** Montant d'achat qui déclenche une remise de panier ; nul pour les autres mécaniques. */
        Integer seuilMontant,
        /** Cumulable avec la remise produit de la vente. */
        boolean cumulable,
        /**
         * Ce que l'officine doit savoir avant de valider — article à prix administré, vente à perte —
         * en phrases ; vide si rien à signaler. Signalé, jamais refusé.
         */
        List<String> alertes) {

    public PromotionDTO(
            Long id,
            String code,
            String libelle,
            TypePromotion type,
            Integer valeur,
            LocalDate dateDebut,
            LocalDate dateFin,
            StatutPromotion statut,
            EtatPromotion etat,
            int priorite,
            int quantiteMin,
            Integer quantiteMaxParVente,
            Integer quantiteMaxTotale,
            long unitesRemisees,
            int nombreProduits,
            List<PromotionProduitDTO> produits,
            String codeBarrePack,
            Integer seuilMontant,
            boolean cumulable) {
        this(
                id,
                code,
                libelle,
                type,
                valeur,
                dateDebut,
                dateFin,
                statut,
                etat,
                priorite,
                quantiteMin,
                quantiteMaxParVente,
                quantiteMaxTotale,
                unitesRemisees,
                nombreProduits,
                produits,
                codeBarrePack,
                seuilMontant,
                cumulable,
                List.of());
    }

    public PromotionDTO(
            Long id,
            String code,
            String libelle,
            TypePromotion type,
            Integer valeur,
            LocalDate dateDebut,
            LocalDate dateFin,
            StatutPromotion statut,
            EtatPromotion etat,
            int priorite,
            int quantiteMin,
            Integer quantiteMaxParVente,
            Integer quantiteMaxTotale,
            long unitesRemisees,
            int nombreProduits,
            List<PromotionProduitDTO> produits,
            String codeBarrePack,
            Integer seuilMontant) {
        this(
                id,
                code,
                libelle,
                type,
                valeur,
                dateDebut,
                dateFin,
                statut,
                etat,
                priorite,
                quantiteMin,
                quantiteMaxParVente,
                quantiteMaxTotale,
                unitesRemisees,
                nombreProduits,
                produits,
                codeBarrePack,
                seuilMontant,
                false);
    }

    public PromotionDTO(
            Long id,
            String code,
            String libelle,
            TypePromotion type,
            Integer valeur,
            LocalDate dateDebut,
            LocalDate dateFin,
            StatutPromotion statut,
            EtatPromotion etat,
            int priorite,
            int quantiteMin,
            Integer quantiteMaxParVente,
            Integer quantiteMaxTotale,
            long unitesRemisees,
            int nombreProduits,
            List<PromotionProduitDTO> produits,
            String codeBarrePack) {
        this(
                id,
                code,
                libelle,
                type,
                valeur,
                dateDebut,
                dateFin,
                statut,
                etat,
                priorite,
                quantiteMin,
                quantiteMaxParVente,
                quantiteMaxTotale,
                unitesRemisees,
                nombreProduits,
                produits,
                codeBarrePack,
                null);
    }

    public PromotionDTO(
            Long id,
            String code,
            String libelle,
            TypePromotion type,
            Integer valeur,
            LocalDate dateDebut,
            LocalDate dateFin,
            StatutPromotion statut,
            EtatPromotion etat,
            int priorite,
            int quantiteMin,
            Integer quantiteMaxParVente,
            Integer quantiteMaxTotale,
            long unitesRemisees,
            int nombreProduits,
            List<PromotionProduitDTO> produits) {
        this(
                id,
                code,
                libelle,
                type,
                valeur,
                dateDebut,
                dateFin,
                statut,
                etat,
                priorite,
                quantiteMin,
                quantiteMaxParVente,
                quantiteMaxTotale,
                unitesRemisees,
                nombreProduits,
                produits,
                (String) null);
    }
}
