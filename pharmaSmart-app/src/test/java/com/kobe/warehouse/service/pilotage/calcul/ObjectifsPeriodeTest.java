package com.kobe.warehouse.service.pilotage.calcul;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.kobe.warehouse.domain.ObjectifPilotage;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pilotage — objectifs ramenés à une tranche")
class ObjectifsPeriodeTest {

    private final ObjectifsPeriode objectifs = ObjectifsPeriode.ranger(
        List.of(
            lireObjectif(IndicateurPilotage.CA_TTC, 9, 3_000),
            lireObjectif(IndicateurPilotage.CA_TTC, 10, 3_100),
            lireObjectif(IndicateurPilotage.TAUX_REMISE, 10, 4)
        )
    );

    @Test
    @DisplayName("un montant se somme mois par mois, au prorata des jours d'un mois entamé")
    void sommerAuProrata() {
        assertThat(objectifs.lireObjectif(IndicateurPilotage.CA_TTC, periode("2026-10-01", "2026-10-31"))).isEqualTo(3_100.0);
        assertThat(objectifs.lireObjectif(IndicateurPilotage.CA_TTC, periode("2026-10-01", "2026-10-10"))).isCloseTo(1_000.0, within(0.01));
        assertThat(objectifs.lireObjectif(IndicateurPilotage.CA_TTC, periode("2026-09-01", "2026-10-10"))).isCloseTo(4_000.0, within(0.01));
    }

    @Test
    @DisplayName("un mois sans objectif laisse la tranche sans objectif ; un taux ne vaut que sur un mois")
    void refuserIncomplet() {
        assertThat(objectifs.lireObjectif(IndicateurPilotage.CA_TTC, periode("2026-08-01", "2026-09-30"))).isNull();
        assertThat(objectifs.lireObjectif(IndicateurPilotage.TAUX_REMISE, periode("2026-10-01", "2026-10-10"))).isEqualTo(4.0);
        assertThat(objectifs.lireObjectif(IndicateurPilotage.TAUX_REMISE, periode("2026-09-01", "2026-10-31"))).isNull();
        assertThat(ObjectifsPeriode.AUCUN.lireObjectif(IndicateurPilotage.CA_TTC, periode("2026-10-01", "2026-10-31"))).isNull();
    }

    private static PeriodeDTO periode(String du, String au) {
        return new PeriodeDTO(LocalDate.parse(du), LocalDate.parse(au));
    }

    private static ObjectifPilotage lireObjectif(IndicateurPilotage indicateur, int mois, double valeur) {
        return new ObjectifPilotage().setIndicateur(indicateur).setAnnee(2026).setMois(mois).setValeur(valeur);
    }
}
