package com.kobe.warehouse.service.promotion;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.Promotion;
import com.kobe.warehouse.domain.enumeration.StatutPromotion;
import com.kobe.warehouse.service.dto.promotion.EtatPromotion;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("EtatPromotion : le statut humain précisé par les dates et le plafond")
class EtatPromotionTest {

    private static final LocalDate JOUR = LocalDate.of(2026, 11, 27);

    private static Promotion validee(Integer plafond) {
        return new Promotion()
                .setStatut(StatutPromotion.VALIDEE)
                .setDateDebut(JOUR.minusDays(2))
                .setDateFin(JOUR.plusDays(2))
                .setQuantiteMaxTotale(plafond);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"BROUILLON, BROUILLON", "SUSPENDUE, SUSPENDUE", "ANNULEE, ANNULEE"})
    @DisplayName("hors validation, l'état est le statut, quelles que soient les dates ou les ventes")
    void statutHumain(StatutPromotion statut, EtatPromotion attendu) {
        assertThat(EtatPromotion.de(validee(5).setStatut(statut), JOUR, 99)).isEqualTo(attendu);
    }

    @Test
    @DisplayName("validée : à venir, en cours, terminée selon la date, bornes incluses")
    void dates() {
        Promotion promotion = validee(null);
        assertThat(EtatPromotion.de(promotion, JOUR.minusDays(3))).isEqualTo(EtatPromotion.A_VENIR);
        assertThat(EtatPromotion.de(promotion, JOUR.minusDays(2))).isEqualTo(EtatPromotion.EN_COURS);
        assertThat(EtatPromotion.de(promotion, JOUR.plusDays(2))).isEqualTo(EtatPromotion.EN_COURS);
        assertThat(EtatPromotion.de(promotion, JOUR.plusDays(3))).isEqualTo(EtatPromotion.TERMINEE);
    }

    @Test
    @DisplayName("en cours et plafond atteint : épuisée ; sous le plafond ou sans plafond : en cours")
    void epuisee() {
        assertThat(EtatPromotion.de(validee(5), JOUR, 4)).isEqualTo(EtatPromotion.EN_COURS);
        assertThat(EtatPromotion.de(validee(5), JOUR, 5)).isEqualTo(EtatPromotion.EPUISEE);
        assertThat(EtatPromotion.de(validee(5), JOUR, 7)).isEqualTo(EtatPromotion.EPUISEE);
        assertThat(EtatPromotion.de(validee(null), JOUR, 1_000)).isEqualTo(EtatPromotion.EN_COURS);
    }

    @Test
    @DisplayName("terminée prime sur épuisée : l'échéance est la raison la plus simple à lire")
    void termineePrime() {
        assertThat(EtatPromotion.de(validee(5), JOUR.plusDays(3), 5)).isEqualTo(EtatPromotion.TERMINEE);
    }
}
