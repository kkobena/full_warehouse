package com.kobe.warehouse.service.pilotage.calcul;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pilotage — calcul des indicateurs et découpage")
class CalculateurIndicateursTest {

    // 10 ventes (dont 2 annulées en plus), 120 000 TTC, 100 000 HT, 6 000 de remises, 30 000 de part TP ;
    // lignes : 100 000 HT, 70 000 de coût HT, 40 articles ; 80 000 d'achats ; 4 jours ouvrés.
    private static final MesuresPilotage MESURES = new MesuresPilotage(10, 2, 120_000, 100_000, 6_000, 30_000, 120_000, 100_000, 70_000, 40, 80_000, 0, 4);

    @Test
    @DisplayName("les ratios se déduisent des sommes")
    void ratios() {
        assertThat(calculer(IndicateurPilotage.PANIER_MOYEN)).isEqualTo(12_000.0);
        assertThat(calculer(IndicateurPilotage.FREQUENTATION)).isEqualTo(2.5);
        assertThat(calculer(IndicateurPilotage.ARTICLES_PAR_VENTE)).isEqualTo(4.0);
        assertThat(calculer(IndicateurPilotage.PRIX_MOYEN_ARTICLE)).isEqualTo(3_000.0);
        assertThat(calculer(IndicateurPilotage.RATIO_VENTES_ACHATS)).isEqualTo(1.5);
    }

    @Test
    @DisplayName("marge, taux et parts en pourcentage")
    void pourcentages() {
        assertThat(calculer(IndicateurPilotage.MARGE_BRUTE)).isEqualTo(30_000.0);
        assertThat(calculer(IndicateurPilotage.TAUX_MARGE)).isEqualTo(30.0);
        assertThat(calculer(IndicateurPilotage.TAUX_REMISE)).isEqualTo(5.0);
        assertThat(calculer(IndicateurPilotage.PART_TIERS_PAYANT)).isEqualTo(25.0);
        assertThat(calculer(IndicateurPilotage.TAUX_ANNULATION)).isCloseTo(16.67, offset(0.01));
        assertThat(calculer(IndicateurPilotage.CA_NET)).isEqualTo(114_000.0);
    }

    @Test
    @DisplayName("un dénominateur nul rend « non calculable », pas une division par zéro")
    void denominateurNul() {
        assertThat(CalculateurIndicateurs.calculer(IndicateurPilotage.PANIER_MOYEN, MesuresPilotage.AUCUNE)).isNull();
        assertThat(CalculateurIndicateurs.calculer(IndicateurPilotage.TAUX_MARGE, MesuresPilotage.AUCUNE)).isNull();
    }

    @Test
    @DisplayName("les indicateurs pas encore alimentés rendent null")
    void indicateursAVenir() {
        assertThat(calculer(IndicateurPilotage.ROTATION_STOCK)).isNull();
        assertThat(calculer(IndicateurPilotage.VENTES_MANQUEES)).isNull();
    }

    @Test
    @DisplayName("le découpage rogne la première et la dernière tranche aux bornes")
    void decoupage() {
        PeriodeDTO periode = new PeriodeDTO(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 10));

        List<PeriodeDTO> mois = DecoupagePeriode.decouper(periode, Granularite.MOIS);
        List<PeriodeDTO> trimestres = DecoupagePeriode.decouper(new PeriodeDTO(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 8, 31)), Granularite.TRIMESTRE);
        List<PeriodeDTO> semaines = DecoupagePeriode.decouper(new PeriodeDTO(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 9)), Granularite.SEMAINE);

        assertThat(mois).containsExactly(
            new PeriodeDTO(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 31)),
            new PeriodeDTO(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)),
            new PeriodeDTO(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10))
        );
        assertThat(trimestres).extracting(PeriodeDTO::au).containsExactly(LocalDate.of(2026, 3, 31), LocalDate.of(2026, 6, 30), LocalDate.of(2026, 8, 31));
        assertThat(semaines).extracting(PeriodeDTO::au).containsExactly(LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 9));
    }

    private static Double calculer(IndicateurPilotage indicateur) {
        return CalculateurIndicateurs.calculer(indicateur, MESURES);
    }
}
