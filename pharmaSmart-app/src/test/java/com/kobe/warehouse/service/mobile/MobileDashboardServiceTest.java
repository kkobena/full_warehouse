package com.kobe.warehouse.service.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.repository.MobileSalesRepository;
import com.kobe.warehouse.repository.MobileSalesRepository.DailyCATrendProjection;
import com.kobe.warehouse.repository.MobileSalesRepository.DailySalesSummaryProjection;
import com.kobe.warehouse.repository.MobileSalesRepository.TopProductProjection;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO.DailyCASummaryDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO.MobileAlertDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO.TopProductDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * L'écran d'accueil de l'application tient en un seul appel : c'est tout l'objet de ce service, et
 * la raison pour laquelle il agrège une demi-douzaine de sources d'un coup.
 *
 * <p>Le chiffre du jour n'a d'intérêt que rapporté à quelque chose. Il l'est ici deux fois : à la
 * veille — utile mais volatile, un lundi ne ressemble pas à un dimanche — et à la moyenne des trente
 * derniers jours, qui lisse la semaine. Ce sont ces deux écarts, plus que le montant lui-même, que
 * le pharmacien lit d'un coup d'œil ; un dénominateur nul ne doit ni les faire exploser ni faire
 * échouer l'écran entier.
 */
@DisplayName("MobileDashboardService — écran d'accueil")
class MobileDashboardServiceTest {

    private static final LocalDate JOUR = LocalDate.of(2026, 3, 10);
    private static final LocalDate VEILLE = LocalDate.of(2026, 3, 9);

    // Doublures créées à l'initialisation des champs, et non dans un @BeforeEach : le délai de
    // quinze secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final MobileAlertService alertService = mock(MobileAlertService.class);
    private final MobileSalesRepository salesRepository = mock(MobileSalesRepository.class);

    private final MobileDashboardService service = new MobileDashboardService(alertService, salesRepository);

    @BeforeEach
    void journeeVide() {
        when(salesRepository.getDailySalesSummary(any())).thenReturn(journee(0, 0));
        when(salesRepository.getAverageCA(any(), anyInt())).thenReturn(0L);
        when(salesRepository.getTopProducts(any(LocalDate.class), anyInt())).thenReturn(List.of());
        when(salesRepository.getCATrend(any(), any())).thenReturn(List.of());
        when(alertService.getAlertsSummary()).thenReturn(List.of());
    }

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre interrogé")
    class PerimetreInterroge {

        @Test
        @DisplayName("la journée demandée est comparée à la veille")
        void comparaisonALaVeille() {
            service.getDashboard(JOUR);

            verify(salesRepository).getDailySalesSummary(JOUR);
            verify(salesRepository).getDailySalesSummary(VEILLE);
        }

        @Test
        @DisplayName("la moyenne de référence porte sur trente jours")
        void moyenneTrenteJours() {
            service.getDashboard(JOUR);

            verify(salesRepository).getAverageCA(JOUR, 30);
        }

        /** La courbe tient sur une semaine : sept jours, jour demandé compris. */
        @Test
        @DisplayName("la courbe couvre les sept derniers jours, jour demandé compris")
        void courbeSurSeptJours() {
            service.getDashboard(JOUR);

            verify(salesRepository).getCATrend(LocalDate.of(2026, 3, 4), JOUR);
        }

        @Test
        @DisplayName("le palmarès du jour se limite à cinq produits")
        void palmaresDeCinq() {
            service.getDashboard(JOUR);

            verify(salesRepository).getTopProducts(JOUR, 5);
        }

        @Test
        @DisplayName("la journée demandée est reportée dans le résultat")
        void journeeReportee() {
            assertThat(service.getDashboard(JOUR).date()).isEqualTo(JOUR);
        }
    }

    // ===== chiffres du jour =====

    @Nested
    @DisplayName("Chiffres du jour")
    class ChiffresDuJour {

        @Test
        @DisplayName("les chiffres de la journée sont repris tels quels")
        void chiffresRepris() {
            when(salesRepository.getDailySalesSummary(JOUR)).thenReturn(
                new DailySalesSummaryProjection(1_000_000L, 42, 38, 23_809L, 900_000L, 100_000L, 400_000L, 40.0)
            );

            MobileDashboardDTO resultat = service.getDashboard(JOUR);

            assertThat(resultat.dailyCA()).isEqualTo(1_000_000L);
            assertThat(resultat.transactionsCount()).isEqualTo(42);
            assertThat(resultat.customersCount()).isEqualTo(38);
            assertThat(resultat.averageBasket()).isEqualTo(23_809L);
            assertThat(resultat.amountCollected()).isEqualTo(900_000L);
            assertThat(resultat.amountCredit()).isEqualTo(100_000L);
            assertThat(resultat.marginAmount()).isEqualTo(400_000L);
            assertThat(resultat.marginPercent()).isEqualTo(40.0);
        }

        @Test
        @DisplayName("la moyenne glissante est reportée à côté du chiffre du jour")
        void moyenneReportee() {
            when(salesRepository.getAverageCA(JOUR, 30)).thenReturn(850_000L);

            assertThat(service.getDashboard(JOUR).averageCA30j()).isEqualTo(850_000L);
        }
    }

    // ===== écarts =====

    @Nested
    @DisplayName("Écarts")
    class Ecarts {

        @Test
        @DisplayName("une journée meilleure que la veille se lit en pourcentage positif")
        void progression() {
            when(salesRepository.getDailySalesSummary(JOUR)).thenReturn(journee(1_200_000L, 100));
            when(salesRepository.getDailySalesSummary(VEILLE)).thenReturn(journee(1_000_000L, 90));

            assertThat(service.getDashboard(JOUR).variationPercent()).isEqualTo(20.0);
        }

        @Test
        @DisplayName("une journée moins bonne se lit en pourcentage négatif")
        void recul() {
            when(salesRepository.getDailySalesSummary(JOUR)).thenReturn(journee(750_000L, 60));
            when(salesRepository.getDailySalesSummary(VEILLE)).thenReturn(journee(1_000_000L, 90));

            assertThat(service.getDashboard(JOUR).variationPercent()).isEqualTo(-25.0);
        }

        @Test
        @DisplayName("l'écart à la moyenne des trente jours se calcule de la même façon")
        void ecartALaMoyenne() {
            when(salesRepository.getDailySalesSummary(JOUR)).thenReturn(journee(1_000_000L, 80));
            when(salesRepository.getAverageCA(JOUR, 30)).thenReturn(800_000L);

            assertThat(service.getDashboard(JOUR).trendVs30j()).isEqualTo(25.0);
        }

        /**
         * Une veille fermée — dimanche, jour férié, premier jour d'exploitation — ne donne aucun
         * écart : il n'y a rien à comparer. Le service rendait cent pour cent, et le tableau de bord
         * comme le widget affichaient « +100 % » en vert, exactement comme une vraie progression.
         */
        @Test
        @DisplayName("une veille sans activité ne rend aucun écart")
        void veilleSansActivite() {
            when(salesRepository.getDailySalesSummary(JOUR)).thenReturn(journee(500_000L, 40));
            when(salesRepository.getDailySalesSummary(VEILLE)).thenReturn(journee(0L, 0));

            assertThat(service.getDashboard(JOUR).variationPercent()).isNull();
        }

        /** Une moyenne glissante encore vide ne dit rien non plus de la tendance. */
        @Test
        @DisplayName("une moyenne des trente jours vide ne rend aucune tendance")
        void moyenneSansHistorique() {
            when(salesRepository.getDailySalesSummary(JOUR)).thenReturn(journee(500_000L, 40));
            when(salesRepository.getAverageCA(JOUR, 30)).thenReturn(0L);

            assertThat(service.getDashboard(JOUR).trendVs30j()).isNull();
        }

        /** Deux journées sans activité se valent : l'écart est nul, non absent. */
        @Test
        @DisplayName("deux journées vides donnent un écart de zéro")
        void deuxJourneesVides() {
            assertThat(service.getDashboard(JOUR).variationPercent()).isEqualTo(0.0);
            assertThat(service.getDashboard(JOUR).trendVs30j()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("l'écart est arrondi au centième")
        void arrondiAuCentieme() {
            when(salesRepository.getDailySalesSummary(JOUR)).thenReturn(journee(4L, 1));
            when(salesRepository.getDailySalesSummary(VEILLE)).thenReturn(journee(3L, 1));

            assertThat(service.getDashboard(JOUR).variationPercent()).isEqualTo(33.33);
        }
    }

    // ===== alertes =====

    @Nested
    @DisplayName("Alertes")
    class Alertes {

        @Test
        @DisplayName("les alertes de l'officine sont reprises avec leur total")
        void alertesReprises() {
            when(alertService.getAlertsSummary()).thenReturn(
                List.of(alerte("STOCK_RUPTURE", 7), alerte("EXPIRY", 4), alerte("CASH_DISCREPANCY", 1))
            );

            MobileDashboardDTO resultat = service.getDashboard(JOUR);

            assertThat(resultat.alerts()).extracting(MobileAlertDTO::type)
                .containsExactly("STOCK_RUPTURE", "EXPIRY", "CASH_DISCREPANCY");
            assertThat(resultat.alertsCount()).isEqualTo(12);
        }

        @Test
        @DisplayName("une officine sans anomalie affiche un compteur à zéro")
        void aucuneAlerte() {
            MobileDashboardDTO resultat = service.getDashboard(JOUR);

            assertThat(resultat.alerts()).isEmpty();
            assertThat(resultat.alertsCount()).isZero();
        }
    }

    // ===== palmarès et courbe =====

    @Nested
    @DisplayName("Palmarès et courbe")
    class PalmaresEtCourbe {

        @Test
        @DisplayName("chaque produit du palmarès garde son rang et son code CIP")
        void palmares() {
            when(salesRepository.getTopProducts(JOUR, 5)).thenReturn(
                List.of(
                    new TopProductProjection(1L, "DOLIPRANE", "CIP1", 500_000L, 120, 1),
                    new TopProductProjection(2L, "EFFERALGAN", "CIP2", 300_000L, 80, 2)
                )
            );

            List<TopProductDTO> palmares = service.getDashboard(JOUR).topProducts();

            assertThat(palmares).extracting(TopProductDTO::name).containsExactly("DOLIPRANE", "EFFERALGAN");
            assertThat(palmares.getFirst().id()).isEqualTo(1L);
            assertThat(palmares.getFirst().codeCip()).isEqualTo("CIP1");
            assertThat(palmares.getFirst().salesAmount()).isEqualTo(500_000L);
            assertThat(palmares.getFirst().quantitySold()).isEqualTo(120);
            assertThat(palmares.getFirst().rank()).isEqualTo(1);
        }

        /** L'axe de la courbe est étroit : le jour de la semaine abrégé y tient, pas la date. */
        @Test
        @DisplayName("chaque point de la courbe porte son jour de la semaine abrégé")
        void courbe() {
            when(salesRepository.getCATrend(any(), any())).thenReturn(
                // Le 9 mars 2026 est un lundi, le 10 un mardi.
                List.of(
                    new DailyCATrendProjection(LocalDate.of(2026, 3, 9), 800_000L, 30),
                    new DailyCATrendProjection(LocalDate.of(2026, 3, 10), 1_000_000L, 42)
                )
            );

            List<DailyCASummaryDTO> courbe = service.getDashboard(JOUR).caTrend();

            assertThat(courbe).extracting(DailyCASummaryDTO::caTotal).containsExactly(800_000L, 1_000_000L);
            assertThat(courbe.getFirst().dayLabel()).startsWith("lun");
            assertThat(courbe.getLast().dayLabel()).startsWith("mar");
            assertThat(courbe.getLast().transactionsCount()).isEqualTo(42);
        }

        @Test
        @DisplayName("une journée sans vente rend un palmarès et une courbe vides")
        void journeeSansVente() {
            MobileDashboardDTO resultat = service.getDashboard(JOUR);

            assertThat(resultat.topProducts()).isEmpty();
            assertThat(resultat.caTrend()).isEmpty();
        }
    }

    // ===== fabriques =====

    private static DailySalesSummaryProjection journee(long caTotal, int ventes) {
        return new DailySalesSummaryProjection(caTotal, ventes, ventes, 0L, caTotal, 0L, 0L, 0.0);
    }

    private static MobileAlertDTO alerte(String type, int nombre) {
        return new MobileAlertDTO(type, "CRITICAL", nombre + " anomalies", nombre, "icone", "#000000");
    }
}
