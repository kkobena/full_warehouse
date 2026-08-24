package com.kobe.warehouse.service.declaration_ca.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ce que le pharmacien saisit avant de simuler une ponction.
 *
 * @param dateDebut début de période, bornes incluses
 * @param dateFin fin de période
 * @param modeCalcul {@code MONTANT_FIXE} ou {@code POURCENTAGE}
 * @param valeur montant en francs, ou taux en pourcentage selon le mode
 * @param plafondParVente part maximale prélevable sur une vente ; 35 % par défaut
 * @param commentaire justification libre, reprise dans l'historique
 */
public record PonctionParamDTO(
    @NotNull LocalDate dateDebut,
    @NotNull LocalDate dateFin,
    @NotNull ModeCalculPonction modeCalcul,
    @NotNull @Positive BigDecimal valeur,
    BigDecimal plafondParVente,
    String commentaire
) {
    /**
     * Une période dont la fin précède le début ne rend rien : la requête ne remonterait aucune
     * vente, et le refus s'expliquerait par une assiette vide plutôt que par la saisie.
     */
    public PonctionParamDTO {
        if (dateDebut != null && dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date de début.");
        }
    }

    /** Plafond retenu, ou celui de l'officine faute de saisie. */
    public BigDecimal plafondEffectif(BigDecimal defautOfficine) {
        return plafondParVente == null ? defautOfficine : plafondParVente;
    }
}
