package com.kobe.warehouse.service.declaration_ca.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Une ponction telle qu'affichée dans l'historique. */
public record PonctionDTO(
    Integer id,
    LocalDate dateDebut,
    LocalDate dateFin,
    ModeCalculPonction modeCalcul,
    BigDecimal valeurSaisie,
    BigDecimal plafondParVente,
    long caReel,
    long caApresExclusions,
    long caDeclare,
    long montantObjectif,
    long montantPonctionne,
    int nombreVentes,
    StatutPonction statut,
    String commentaire,
    String auteur,
    LocalDateTime creeLe,
    LocalDateTime valideLe,
    LocalDateTime annuleLe
) {
    /** Taux réellement appliqué au chiffre d'affaires, ce que le pourcentage saisi ne dit pas. */
    public BigDecimal tauxEffectif() {
        if (caApresExclusions <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(montantPonctionne)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(caApresExclusions), 2, java.math.RoundingMode.HALF_UP);
    }
}
