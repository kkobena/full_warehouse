package com.kobe.warehouse.service.dto.promotion;

import com.kobe.warehouse.domain.Promotion;
import java.time.LocalDate;

/** Ce que l'écran affiche : le statut humain, précisé par les dates pour une promotion validée. */
public enum EtatPromotion {
    BROUILLON,
    A_VENIR,
    EN_COURS,
    /** En cours, mais son plafond total d'unités est atteint : le comptoir ne la pose plus. */
    EPUISEE,
    TERMINEE,
    SUSPENDUE,
    ANNULEE;

    public static EtatPromotion de(Promotion promotion, LocalDate aujourdhui) {
        return de(promotion, aujourdhui, 0);
    }

    public static EtatPromotion de(Promotion promotion, LocalDate aujourdhui, long unitesRemisees) {
        return switch (promotion.getStatut()) {
            case BROUILLON -> BROUILLON;
            case SUSPENDUE -> SUSPENDUE;
            case ANNULEE -> ANNULEE;
            case VALIDEE -> {
                if (aujourdhui.isBefore(promotion.getDateDebut())) {
                    yield A_VENIR;
                }
                if (aujourdhui.isAfter(promotion.getDateFin())) {
                    yield TERMINEE;
                }
                Integer plafond = promotion.getQuantiteMaxTotale();
                yield plafond != null && unitesRemisees >= plafond ? EPUISEE : EN_COURS;
            }
        };
    }
}
