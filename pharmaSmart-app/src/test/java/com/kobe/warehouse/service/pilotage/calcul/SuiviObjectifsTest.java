package com.kobe.warehouse.service.pilotage.calcul;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.service.dto.pilotage.ProjectionObjectifDTO;
import com.kobe.warehouse.service.dto.pilotage.SuiviMoisDTO;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pilotage — suivi des objectifs et projection de fin de mois")
class SuiviObjectifsTest {

    private static final LocalDate LE_10_OCTOBRE = LocalDate.of(2026, 10, 10);

    @Test
    @DisplayName("projection d'un montant : le réalisé à date suit le profil du même mois N-1")
    void projeterSurLeProfilN1() {
        ProjectionObjectifDTO projection = SuiviObjectifs.projeter(IndicateurPilotage.CA_TTC, 1_500.0, lireCa(400), lireCa(300), lireCa(700), LE_10_OCTOBRE);

        assertThat(projection.projection()).isCloseTo(1_333.33, within(0.01));
        assertThat(projection.atteinteProjetee()).isCloseTo(88.89, within(0.01));
        assertThat(projection.tenu()).isFalse();
        assertThat(projection.methode()).isEqualTo("D'après octobre 2025 : au même jour, 30 % du mois était fait.");
    }

    @Test
    @DisplayName("sans vente N-1 au même jour, au rythme des jours écoulés")
    void projeterAuRythme() {
        ProjectionObjectifDTO projection = SuiviObjectifs.projeter(IndicateurPilotage.CA_TTC, null, lireCa(100), MesuresPilotage.AUCUNE, lireCa(700), LE_10_OCTOBRE);

        assertThat(projection.projection()).isEqualTo(310.0);
        assertThat(projection.atteinteProjetee()).isNull();
        assertThat(projection.methode()).startsWith("Au rythme des jours écoulés");
    }

    @Test
    @DisplayName("un taux à ne pas dépasser : projeté à sa valeur à date, tenu tant qu'il reste sous le plafond")
    void projeterUnPlafond() {
        MesuresPilotage remise5Pct = new MesuresPilotage(1, 0, 1_000, 1_000, 50, 0, 1_000, 1_000, 0, 1, 0, 0, 1);

        ProjectionObjectifDTO projection = SuiviObjectifs.projeter(IndicateurPilotage.TAUX_REMISE, 4.0, remise5Pct, lireCa(300), lireCa(700), LE_10_OCTOBRE);

        assertThat(projection.projection()).isEqualTo(5.0);
        assertThat(projection.tenu()).isFalse();
        assertThat(SuiviObjectifs.suivre(IndicateurPilotage.TAUX_REMISE, 9, 6.0, 5.0, false).tenu()).isTrue();
    }

    @Test
    @DisplayName("mois suivi : écart, atteinte ; rien à juger sans objectif")
    void suivre() {
        SuiviMoisDTO tenu = SuiviObjectifs.suivre(IndicateurPilotage.CA_TTC, 1, 900.0, 1_000.0, false);

        assertThat(tenu.ecart()).isEqualTo(100.0);
        assertThat(tenu.atteinte()).isCloseTo(111.11, within(0.01));
        assertThat(tenu.tenu()).isTrue();
        assertThat(SuiviObjectifs.suivre(IndicateurPilotage.CA_TTC, 2, null, 1_000.0, false).tenu()).isNull();
    }

    private static MesuresPilotage lireCa(long caTtc) {
        return new MesuresPilotage(1, 0, caTtc, caTtc, 0, 0, caTtc, caTtc, 0, 1, 0, 0, 1);
    }
}
