package com.kobe.warehouse.service.dto.promotion;

import com.kobe.warehouse.domain.enumeration.TypePromotion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Création ou modification d'une promotion. Le magasin n'y figure pas : c'est celui de l'utilisateur. */
public record PromotionFormDTO(
        @NotBlank @Size(max = 100) String libelle,
        @NotNull TypePromotion type,
        Integer valeur,
        @NotNull LocalDate dateDebut,
        @NotNull LocalDate dateFin,
        int priorite,
        @Min(1) int quantiteMin,
        Integer quantiteMaxParVente,
        Integer quantiteMaxTotale,
        @NotEmpty @Valid List<PromotionProduitFormDTO> produits,
        /** Le montant d'achat qui déclenche une remise de panier ; ignoré pour les autres mécaniques. */
        Integer seuilMontant,
        /** Cumulable avec la remise produit de la vente ; nul : non. */
        Boolean cumulable) {

    public PromotionFormDTO(
            String libelle,
            TypePromotion type,
            Integer valeur,
            LocalDate dateDebut,
            LocalDate dateFin,
            int priorite,
            int quantiteMin,
            Integer quantiteMaxParVente,
            Integer quantiteMaxTotale,
            List<PromotionProduitFormDTO> produits,
            Integer seuilMontant) {
        this(
                libelle,
                type,
                valeur,
                dateDebut,
                dateFin,
                priorite,
                quantiteMin,
                quantiteMaxParVente,
                quantiteMaxTotale,
                produits,
                seuilMontant,
                null);
    }

    public PromotionFormDTO(
            String libelle,
            TypePromotion type,
            Integer valeur,
            LocalDate dateDebut,
            LocalDate dateFin,
            int priorite,
            int quantiteMin,
            Integer quantiteMaxParVente,
            Integer quantiteMaxTotale,
            List<PromotionProduitFormDTO> produits) {
        this(
                libelle,
                type,
                valeur,
                dateDebut,
                dateFin,
                priorite,
                quantiteMin,
                quantiteMaxParVente,
                quantiteMaxTotale,
                produits,
                (Integer) null);
    }
}
