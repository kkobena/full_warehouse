package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pilotage — période de référence")
class PeriodeComparaisonServiceImplTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 9);

    private final PeriodeComparaisonServiceImpl service = new PeriodeComparaisonServiceImpl();

    @Test
    @DisplayName("le mois en cours se compare à date au même mois N-1")
    void moisEnCoursADate() {
        ComparaisonPeriodesDTO comparaison = comparer(octobre(), TypeComparaison.MEME_PERIODE_N_1, true);

        assertThat(comparaison.aDate()).isTrue();
        assertThat(comparaison.periode()).isEqualTo(periode("2026-10-01", "2026-10-09"));
        assertThat(comparaison.reference()).isEqualTo(periode("2025-10-01", "2025-10-09"));
    }

    @Test
    @DisplayName("sans « à date », le mois en cours se compare au mois N-1 entier")
    void moisEnCoursEntier() {
        ComparaisonPeriodesDTO comparaison = comparer(octobre(), TypeComparaison.MEME_PERIODE_N_1, false);

        assertThat(comparaison.aDate()).isFalse();
        assertThat(comparaison.reference()).isEqualTo(periode("2025-10-01", "2025-10-31"));
    }

    @Test
    @DisplayName("la période précédente d'un mois en cours est le début du mois d'avant, à date")
    void periodePrecedenteDUnMois() {
        assertThat(comparer(octobre(), TypeComparaison.PERIODE_PRECEDENTE, true).reference()).isEqualTo(periode("2026-09-01", "2026-09-09"));
    }

    @Test
    @DisplayName("la période précédente d'un mois entier de 31 jours tombe sur le dernier jour du mois visé")
    void periodePrecedenteFinDeMois() {
        RequetePilotageDTO mars = requete(periode("2026-03-01", "2026-03-31"), TypeComparaison.PERIODE_PRECEDENTE, true);

        assertThat(service.comparer(mars, AUJOURDHUI).reference()).isEqualTo(periode("2026-02-01", "2026-02-28"));
    }

    @Test
    @DisplayName("la période précédente d'une période libre recule de sa longueur")
    void periodePrecedenteLibre() {
        RequetePilotageDTO quinzaine = requete(periode("2026-09-05", "2026-09-18"), TypeComparaison.PERIODE_PRECEDENTE, true);

        assertThat(service.comparer(quinzaine, AUJOURDHUI).reference()).isEqualTo(periode("2026-08-22", "2026-09-04"));
    }

    @Test
    @DisplayName("N-3 recule de trois ans ; le 29 février tombe sur le 28")
    void anneeNMoinsK() {
        RequetePilotageDTO fevrier2028 = new RequetePilotageDTO(
            LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29), TypeComparaison.ANNEE_N_MOINS_K, 3, null, null, false, null, null
        );

        assertThat(service.comparer(fevrier2028, AUJOURDHUI).reference()).isEqualTo(periode("2025-02-01", "2025-02-28"));
    }

    @Test
    @DisplayName("au-delà de N-5, la comparaison est refusée")
    void auDelaDeCinqAns() {
        RequetePilotageDTO trop = new RequetePilotageDTO(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), TypeComparaison.ANNEE_N_MOINS_K, 6, null, null, false, null, null
        );

        assertThatThrownBy(() -> service.comparer(trop, AUJOURDHUI)).isInstanceOf(GenericError.class);
    }

    @Test
    @DisplayName("sans comparaison, pas de référence")
    void sansComparaison() {
        assertThat(comparer(octobre(), TypeComparaison.AUCUNE, true).reference()).isNull();
    }

    @Test
    @DisplayName("une période entièrement passée n'est jamais tronquée")
    void periodePassee() {
        RequetePilotageDTO septembre = requete(periode("2026-09-01", "2026-09-30"), TypeComparaison.MEME_PERIODE_N_1, true);

        ComparaisonPeriodesDTO comparaison = service.comparer(septembre, AUJOURDHUI);

        assertThat(comparaison.aDate()).isFalse();
        assertThat(comparaison.periode()).isEqualTo(periode("2026-09-01", "2026-09-30"));
    }

    private ComparaisonPeriodesDTO comparer(PeriodeDTO periode, TypeComparaison type, boolean aDate) {
        return service.comparer(requete(periode, type, aDate), AUJOURDHUI);
    }

    private static RequetePilotageDTO requete(PeriodeDTO periode, TypeComparaison type, boolean aDate) {
        return new RequetePilotageDTO(periode.du(), periode.au(), type, null, null, null, aDate, null, null);
    }

    private static PeriodeDTO octobre() {
        return periode("2026-10-01", "2026-10-31");
    }

    private static PeriodeDTO periode(String du, String au) {
        return new PeriodeDTO(LocalDate.parse(du), LocalDate.parse(au));
    }
}
