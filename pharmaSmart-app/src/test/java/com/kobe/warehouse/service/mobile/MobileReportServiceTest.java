package com.kobe.warehouse.service.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.repository.MobileSalesRepository;
import com.kobe.warehouse.repository.MobileSalesRepository.DailySalesSummaryProjection;
import com.kobe.warehouse.repository.MobileSalesRepository.UserSalesSummaryProjection;
import com.kobe.warehouse.service.dto.mobile.DailyDigestDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO.MobileAlertDTO;
import com.kobe.warehouse.service.dto.mobile.UserPerformanceDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Ce service alimente les notifications poussées : le résumé du soir adressé au titulaire, et le
 * bilan individuel adressé à chaque vendeur.
 *
 * <p>Ce sont des chiffres que leur destinataire reçoit sans les avoir demandés et sans pouvoir les
 * recouper — il n'a pas l'écran sous les yeux. Un écart mal calculé ne sera donc pas rattrapé par la
 * lecture : d'où la même précaution qu'ailleurs sur la veille sans activité, et la vérification que
 * le bilan d'un vendeur se compare bien à sa propre veille, non à celle de l'officine.
 */
@DisplayName("MobileReportService — résumés poussés en notification")
class MobileReportServiceTest {

    private static final LocalDate JOUR = LocalDate.of(2026, 3, 10);
    private static final LocalDate VEILLE = LocalDate.of(2026, 3, 9);
    private static final int VENDEUR = 7;

    // Doublures créées à l'initialisation des champs, et non dans un @BeforeEach : le délai de
    // quinze secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final MobileAlertService alertService = mock(MobileAlertService.class);
    private final MobileSalesRepository salesRepository = mock(MobileSalesRepository.class);

    private final MobileReportService service = new MobileReportService(alertService, salesRepository);

    @BeforeEach
    void journeeVide() {
        when(salesRepository.getDailySalesSummary(any())).thenReturn(journee(0L, 0));
        when(salesRepository.getUserSalesSummary(anyInt(), any())).thenReturn(vendeur(0L, 0));
        when(salesRepository.getAverageCA(any(), anyInt())).thenReturn(0L);
        when(salesRepository.getUserName(anyInt())).thenReturn("Jean Dupont");
        when(alertService.getAlertsSummary()).thenReturn(List.of());
    }

    // ===== résumé du titulaire =====

    @Nested
    @DisplayName("Résumé quotidien du titulaire")
    class ResumeQuotidien {

        @Test
        @DisplayName("le résumé reprend les chiffres de la journée")
        void chiffresDeLaJournee() {
            when(salesRepository.getDailySalesSummary(JOUR)).thenReturn(
                new DailySalesSummaryProjection(1_000_000L, 42, 38, 23_809L, 900_000L, 100_000L, 400_000L, 40.0)
            );

            DailyDigestDTO resume = service.generateDailyDigest(JOUR);

            assertThat(resume.getTotalCA()).isEqualTo(1_000_000L);
            assertThat(resume.getTransactionCount()).isEqualTo(42);
            assertThat(resume.getCustomersCount()).isEqualTo(38);
            assertThat(resume.getAverageBasket()).isEqualTo(23_809L);
        }

        @Test
        @DisplayName("la journée est comparée à la veille et à la moyenne de trente jours")
        void deuxReferences() {
            when(salesRepository.getDailySalesSummary(JOUR)).thenReturn(journee(1_200_000L, 50));
            when(salesRepository.getDailySalesSummary(VEILLE)).thenReturn(journee(1_000_000L, 45));
            when(salesRepository.getAverageCA(JOUR, 30)).thenReturn(800_000L);

            DailyDigestDTO resume = service.generateDailyDigest(JOUR);

            assertThat(resume.getVariation()).isEqualTo(20.0);
            assertThat(resume.getAverageCA30j()).isEqualTo(800_000L);
            assertThat(resume.getTrendVs30j()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("une veille sans activité annonce cent pour cent plutôt qu'un infini")
        void veilleSansActivite() {
            when(salesRepository.getDailySalesSummary(JOUR)).thenReturn(journee(500_000L, 20));

            assertThat(service.generateDailyDigest(JOUR).getVariation()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("une journée fermée des deux côtés donne un écart nul")
        void deuxJourneesVides() {
            assertThat(service.generateDailyDigest(JOUR).getVariation()).isZero();
        }

        @Test
        @DisplayName("le nombre d'anomalies annoncé additionne toutes les alertes")
        void nombreDAnomalies() {
            when(alertService.getAlertsSummary()).thenReturn(
                List.of(alerte("STOCK_RUPTURE", 7), alerte("INVOICE_OVERDUE", 3))
            );

            assertThat(service.generateDailyDigest(JOUR).getAlertsCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("une officine sans anomalie n'en annonce aucune")
        void aucuneAnomalie() {
            assertThat(service.generateDailyDigest(JOUR).getAlertsCount()).isZero();
        }
    }

    // ===== bilan d'un vendeur =====

    @Nested
    @DisplayName("Bilan individuel d'un vendeur")
    class BilanDuVendeur {

        /**
         * Le bilan d'un vendeur se compare à <b>sa</b> veille, pas à celle de l'officine : comparer à
         * l'officine ferait passer un bon vendeur pour un mauvais les jours de forte affluence.
         */
        @Test
        @DisplayName("le vendeur est comparé à sa propre veille")
        void comparaisonASaPropreVeille() {
            service.getUserPerformance(VENDEUR, JOUR);

            verify(salesRepository).getUserSalesSummary(VENDEUR, JOUR);
            verify(salesRepository).getUserSalesSummary(VENDEUR, VEILLE);
        }

        @Test
        @DisplayName("le bilan porte le nom du vendeur et ses chiffres")
        void nomEtChiffres() {
            when(salesRepository.getUserSalesSummary(VENDEUR, JOUR)).thenReturn(
                new UserSalesSummaryProjection(600_000L, 25, 24_000L, 180_000L, 30.0)
            );
            when(salesRepository.getUserName(VENDEUR)).thenReturn("Awa Koné");

            UserPerformanceDTO bilan = service.getUserPerformance(VENDEUR, JOUR);

            assertThat(bilan.getUserId()).isEqualTo(7L);
            assertThat(bilan.getUserName()).isEqualTo("Awa Koné");
            assertThat(bilan.getTotalCA()).isEqualTo(600_000L);
            assertThat(bilan.getSalesCount()).isEqualTo(25);
            assertThat(bilan.getAverageBasket()).isEqualTo(24_000L);
            assertThat(bilan.getMarginAmount()).isEqualTo(180_000L);
            assertThat(bilan.getMarginPercent()).isEqualTo(30.0);
        }

        @Test
        @DisplayName("la progression du vendeur se lit en pourcentage")
        void progression() {
            when(salesRepository.getUserSalesSummary(VENDEUR, JOUR)).thenReturn(vendeur(600_000L, 25));
            when(salesRepository.getUserSalesSummary(VENDEUR, VEILLE)).thenReturn(vendeur(500_000L, 22));

            assertThat(service.getUserPerformance(VENDEUR, JOUR).getVariationVsPreviousDay()).isEqualTo(20.0);
        }

        /** Un vendeur qui n'était pas là la veille : cent pour cent, et non une division par zéro. */
        @Test
        @DisplayName("un vendeur absent la veille annonce cent pour cent")
        void absentLaVeille() {
            when(salesRepository.getUserSalesSummary(VENDEUR, JOUR)).thenReturn(vendeur(600_000L, 25));

            assertThat(service.getUserPerformance(VENDEUR, JOUR).getVariationVsPreviousDay()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("un vendeur absent les deux jours affiche un écart nul")
        void absentLesDeuxJours() {
            assertThat(service.getUserPerformance(VENDEUR, JOUR).getVariationVsPreviousDay()).isZero();
        }

        @Test
        @DisplayName("l'écart du vendeur est arrondi au centième")
        void arrondiAuCentieme() {
            when(salesRepository.getUserSalesSummary(VENDEUR, JOUR)).thenReturn(vendeur(4L, 1));
            when(salesRepository.getUserSalesSummary(VENDEUR, VEILLE)).thenReturn(vendeur(3L, 1));

            assertThat(service.getUserPerformance(VENDEUR, JOUR).getVariationVsPreviousDay()).isEqualTo(33.33);
        }
    }

    // ===== fabriques =====

    private static DailySalesSummaryProjection journee(long caTotal, int ventes) {
        return new DailySalesSummaryProjection(caTotal, ventes, ventes, 0L, caTotal, 0L, 0L, 0.0);
    }

    private static UserSalesSummaryProjection vendeur(long caTotal, int ventes) {
        return new UserSalesSummaryProjection(caTotal, ventes, 0L, 0L, 0.0);
    }

    private static MobileAlertDTO alerte(String type, int nombre) {
        return new MobileAlertDTO(type, "CRITICAL", nombre + " anomalies", nombre, "icone", "#000000");
    }
}
