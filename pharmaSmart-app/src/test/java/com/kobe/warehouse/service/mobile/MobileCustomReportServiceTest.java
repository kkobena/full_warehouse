package com.kobe.warehouse.service.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.repository.MobileSalesRepository;
import com.kobe.warehouse.repository.MobileSalesRepository.DailyCATrendProjection;
import com.kobe.warehouse.repository.MobileSalesRepository.PaymentMethodProjection;
import com.kobe.warehouse.repository.MobileSalesRepository.SalesSummaryProjection;
import com.kobe.warehouse.repository.MobileSalesRepository.TopProductProjection;
import com.kobe.warehouse.service.dto.mobile.CustomReportMetricDTO;
import com.kobe.warehouse.service.dto.mobile.CustomReportMetricDTO.ChartDataPointDTO;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Le rapport personnalisé de l'application mobile laisse le pharmacien composer son écran : il
 * choisit les indicateurs qui l'intéressent, et le service les produit un à un pour la période
 * demandée.
 *
 * <p>Ce qui s'y joue tient en deux points. La <b>tendance</b> d'abord : chaque indicateur se compare
 * à la période précédente, de même longueur, immédiatement antérieure. Une fenêtre mal calculée
 * comparerait un mois à trois semaines, et la flèche verte ou rouge affichée à côté du chiffre
 * mentirait — c'est pourtant elle, plus que la valeur absolue, que le pharmacien regarde.
 *
 * <p>La <b>robustesse</b> ensuite. Les codes d'indicateurs viennent d'une configuration enregistrée
 * sur le téléphone : un code devenu obsolète après une mise à jour ne doit pas faire échouer tout
 * l'écran, seulement disparaître. Et une période sans aucune vente — une officine fermée, un
 * nouveau poste — ne doit pas produire de division par zéro là où le service divise le chiffre
 * d'affaires par le nombre de tickets.
 */
@DisplayName("MobileCustomReportService — indicateurs du rapport personnalisé")
class MobileCustomReportServiceTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 3, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 3, 31);

    private final MobileSalesRepository salesRepository = mock(MobileSalesRepository.class);
    private final AppConfigurationService appConfigurationService = mock(AppConfigurationService.class);

    private final MobileCustomReportService service = new MobileCustomReportService(salesRepository, appConfigurationService);

    @BeforeEach
    void setUp() {
        when(salesRepository.getSalesSummary(any(), any())).thenReturn(resume(0, 0, 0));
        when(salesRepository.getCATrend(any(), any())).thenReturn(List.of());
        when(salesRepository.getTopProducts(any(), any(), anyInt())).thenReturn(List.of());
        when(salesRepository.getPaymentMethodsSummary(any(), any())).thenReturn(List.of());
        when(salesRepository.getCustomersCount(any(), any())).thenReturn(0);
        when(appConfigurationService.getDevise()).thenReturn("FCFA");
    }

    // ===== période de comparaison =====

    @Nested
    @DisplayName("Période de comparaison")
    class PeriodeDeComparaison {

        /**
         * Mars fait trente-et-un jours : la période de référence doit couvrir les trente-et-un jours
         * qui précèdent le 1er mars, soit du 29 janvier au 28 février. Comparer un mois à une durée
         * différente fausse la flèche affichée à côté du chiffre.
         */
        @Test
        @DisplayName("la période de référence a la même durée et précède immédiatement")
        void periodeDeMemeDuree() {
            service.generateMetrics(List.of("CA"), DEBUT, FIN);

            verify(salesRepository).getSalesSummary(DEBUT, FIN);
            verify(salesRepository).getSalesSummary(LocalDate.of(2026, 1, 29), LocalDate.of(2026, 2, 28));
        }

        @Test
        @DisplayName("une période d'un seul jour se compare à la veille")
        void periodeDUnJour() {
            LocalDate jour = LocalDate.of(2026, 3, 15);

            service.generateMetrics(List.of("CA"), jour, jour);

            verify(salesRepository).getSalesSummary(jour.minusDays(1), jour.minusDays(1));
        }

        @Test
        @DisplayName("une période d'une semaine se compare à la semaine précédente")
        void periodeDUneSemaine() {
            LocalDate lundi = LocalDate.of(2026, 3, 9);
            LocalDate dimanche = LocalDate.of(2026, 3, 15);

            service.generateMetrics(List.of("CA"), lundi, dimanche);

            verify(salesRepository).getSalesSummary(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8));
        }
    }

    // ===== tendance =====

    @Nested
    @DisplayName("Tendance")
    class Tendance {

        @Test
        @DisplayName("une progression se lit en pourcentage positif")
        void progression() {
            when(salesRepository.getSalesSummary(any(), any())).thenReturn(resume(1_000_000, 80, 250_000));
            when(salesRepository.getSalesSummary(DEBUT, FIN)).thenReturn(resume(1_200_000, 100, 300_000));

            assertThat(indicateur("CA").trend()).isEqualTo(20.0);
        }

        @Test
        @DisplayName("un recul se lit en pourcentage négatif")
        void recul() {
            when(salesRepository.getSalesSummary(any(), any())).thenReturn(resume(1_000_000, 80, 250_000));
            when(salesRepository.getSalesSummary(DEBUT, FIN)).thenReturn(resume(750_000, 60, 180_000));

            assertThat(indicateur("CA").trend()).isEqualTo(-25.0);
        }

        @Test
        @DisplayName("la tendance est arrondie au centième")
        void arrondieAuCentieme() {
            when(salesRepository.getSalesSummary(any(), any())).thenReturn(resume(3, 1, 1));
            when(salesRepository.getSalesSummary(DEBUT, FIN)).thenReturn(resume(4, 1, 1));

            assertThat(indicateur("CA").trend()).isEqualTo(33.33);
        }

        /**
         * Une officine qui ouvre n'a pas de période précédente. Diviser par zéro n'a pas de sens :
         * le service annonce cent pour cent quand il y a quelque chose, et zéro quand il n'y a rien —
         * plutôt qu'un infini que l'écran ne saurait pas afficher.
         */
        @Test
        @DisplayName("sans période précédente, la tendance ne se divise pas par zéro")
        void sansPeriodePrecedente() {
            when(salesRepository.getSalesSummary(any(), any())).thenReturn(resume(0, 0, 0));
            when(salesRepository.getSalesSummary(DEBUT, FIN)).thenReturn(resume(1_000_000, 80, 250_000));

            assertThat(indicateur("CA").trend()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("deux périodes vides donnent une tendance nulle")
        void deuxPeriodesVides() {
            assertThat(indicateur("CA").trend()).isZero();
        }
    }

    // ===== indicateurs =====

    @Nested
    @DisplayName("Indicateurs")
    class Indicateurs {

        @Test
        @DisplayName("le chiffre d'affaires porte sa courbe journalière")
        void chiffreDAffaires() {
            when(salesRepository.getSalesSummary(DEBUT, FIN)).thenReturn(resume(1_200_000, 100, 300_000));
            when(salesRepository.getCATrend(DEBUT, FIN)).thenReturn(
                List.of(jour(LocalDate.of(2026, 3, 1), 40_000), jour(LocalDate.of(2026, 3, 2), 55_000))
            );

            CustomReportMetricDTO indicateur = indicateur("CA");

            assertThat(indicateur.metricName()).isEqualTo("Chiffre d'affaires");
            assertThat(indicateur.value()).contains("1").contains("200").contains("FCFA");
            assertThat(indicateur.chartData()).extracting(ChartDataPointDTO::label).containsExactly("2026-03-01", "2026-03-02");
            assertThat(indicateur.chartData()).extracting(ChartDataPointDTO::value).containsExactly(40_000.0, 55_000.0);
        }

        @Test
        @DisplayName("le nombre de ventes se lit tel quel")
        void nombreDeVentes() {
            when(salesRepository.getSalesSummary(DEBUT, FIN)).thenReturn(resume(1_200_000, 143, 300_000));

            assertThat(indicateur("TRANSACTIONS").value()).isEqualTo("143");
        }

        @Test
        @DisplayName("le panier moyen rapporte le chiffre d'affaires aux tickets")
        void panierMoyen() {
            when(salesRepository.getSalesSummary(DEBUT, FIN)).thenReturn(resume(1_200_000, 100, 300_000));

            assertThat(indicateur("AVERAGE_BASKET").value()).contains("12").contains("000");
        }

        /** Une officine fermée sur la période n'a aucun ticket : la division doit être évitée. */
        @Test
        @DisplayName("sans ticket, le panier moyen vaut zéro plutôt que de diviser par zéro")
        void panierMoyenSansTicket() {
            when(salesRepository.getSalesSummary(DEBUT, FIN)).thenReturn(resume(0, 0, 0));

            assertThat(indicateur("AVERAGE_BASKET").value()).contains("0");
        }

        @Test
        @DisplayName("la marge indique aussi sa part du chiffre d'affaires")
        void marge() {
            when(salesRepository.getSalesSummary(DEBUT, FIN)).thenReturn(resume(1_000_000, 100, 300_000));

            CustomReportMetricDTO indicateur = indicateur("MARGIN");

            assertThat(indicateur.details()).contains("30").contains("% du CA");
        }

        @Test
        @DisplayName("sans chiffre d'affaires, la part de marge ne se divise pas par zéro")
        void margeSansChiffreDAffaires() {
            assertThat(indicateur("MARGIN").details()).contains("0").contains("% du CA");
        }

        /** La devise se lit dans la configuration : elle était écrite en dur dans le format. */
        @Test
        @DisplayName("les montants portent la devise configurée pour l'officine")
        void deviseConfiguree() {
            when(appConfigurationService.getDevise()).thenReturn("EUR");
            when(salesRepository.getSalesSummary(DEBUT, FIN)).thenReturn(resume(1_200_000, 100, 300_000));

            assertThat(indicateur("CA").value()).endsWith("EUR");
        }

        @Test
        @DisplayName("les meilleurs produits alimentent la courbe et le détail")
        void meilleursProduits() {
            when(salesRepository.getTopProducts(any(), any(), anyInt())).thenReturn(
                List.of(produit("DOLIPRANE", 500_000), produit("EFFERALGAN", 300_000))
            );

            CustomReportMetricDTO indicateur = indicateur("TOP_PRODUCTS");

            assertThat(indicateur.value()).isEqualTo("2");
            assertThat(indicateur.chartData()).extracting(ChartDataPointDTO::label).containsExactly("DOLIPRANE", "EFFERALGAN");
            // Le séparateur de milliers dépend de la locale : on ne fige que les chiffres.
            assertThat(indicateur.details()).containsPattern("DOLIPRANE: 500.000").containsPattern("EFFERALGAN: 300.000");
        }

        /** Le détail tient sur une tuile de téléphone : au-delà de cinq lignes, il déborderait. */
        @Test
        @DisplayName("le détail se limite aux cinq premiers produits")
        void detailLimiteACinq() {
            when(salesRepository.getTopProducts(any(), any(), anyInt())).thenReturn(
                List.of(
                    produit("P1", 900_000), produit("P2", 800_000), produit("P3", 700_000),
                    produit("P4", 600_000), produit("P5", 500_000), produit("P6", 400_000),
                    produit("P7", 300_000)
                )
            );

            CustomReportMetricDTO indicateur = indicateur("TOP_PRODUCTS");

            assertThat(indicateur.value()).isEqualTo("7");
            assertThat(indicateur.chartData()).hasSize(7);
            assertThat(indicateur.details().lines()).hasSize(5);
            assertThat(indicateur.details()).doesNotContain("P6");
        }

        @Test
        @DisplayName("le palmarès demande les dix premiers produits")
        void dixPremiersProduits() {
            service.generateMetrics(List.of("TOP_PRODUCTS"), DEBUT, FIN);

            verify(salesRepository).getTopProducts(DEBUT, FIN, 10);
        }

        @Test
        @DisplayName("les modes de paiement alimentent la répartition")
        void modesDePaiement() {
            when(salesRepository.getPaymentMethodsSummary(any(), any())).thenReturn(
                List.of(paiement("ESPECE", 800_000), paiement("WAVE", 200_000))
            );

            CustomReportMetricDTO indicateur = indicateur("PAYMENT_METHODS");

            assertThat(indicateur.value()).isEqualTo("2");
            assertThat(indicateur.chartData()).extracting(ChartDataPointDTO::label).containsExactly("ESPECE", "WAVE");
        }

        @Test
        @DisplayName("les clients uniques se comptent sur la période")
        void clientsUniques() {
            when(salesRepository.getCustomersCount(DEBUT, FIN)).thenReturn(87);

            assertThat(indicateur("CUSTOMER_STATS").value()).isEqualTo("87");
        }
    }

    // ===== composition de l'écran =====

    @Nested
    @DisplayName("Composition de l'écran")
    class CompositionDeLEcran {

        @Test
        @DisplayName("chaque indicateur demandé est rendu sous son code")
        void indicateursDemandes() {
            Map<String, CustomReportMetricDTO> indicateurs = service.generateMetrics(
                List.of("CA", "TRANSACTIONS", "MARGIN"),
                DEBUT,
                FIN
            );

            assertThat(indicateurs).containsOnlyKeys("CA", "TRANSACTIONS", "MARGIN");
            assertThat(indicateurs.get("CA").metricCode()).isEqualTo("CA");
        }

        /**
         * Les codes viennent d'une configuration enregistrée sur le téléphone. Un code retiré par une
         * mise à jour du serveur ne doit pas faire échouer l'écran entier : il disparaît, les autres
         * restent.
         */
        @Test
        @DisplayName("un code inconnu est ignoré sans emporter les autres")
        void codeInconnuIgnore() {
            Map<String, CustomReportMetricDTO> indicateurs = service.generateMetrics(
                List.of("CA", "INDICATEUR_RETIRE", "TRANSACTIONS"),
                DEBUT,
                FIN
            );

            assertThat(indicateurs).containsOnlyKeys("CA", "TRANSACTIONS");
        }

        @Test
        @DisplayName("aucun indicateur demandé rend un écran vide")
        void aucunIndicateurDemande() {
            assertThat(service.generateMetrics(List.of(), DEBUT, FIN)).isEmpty();
        }

        /** Les dix codes de l'énumération doivent tous produire quelque chose, pas une exception. */
        @Test
        @DisplayName("tous les codes connus produisent un indicateur")
        void tousLesCodesConnus() {
            List<String> tousLesCodes = java.util.Arrays.stream(MobileCustomReportService.MetricCode.values())
                .map(Enum::name)
                .toList();

            Map<String, CustomReportMetricDTO> indicateurs = service.generateMetrics(tousLesCodes, DEBUT, FIN);

            assertThat(indicateurs).hasSize(tousLesCodes.size());
            assertThat(indicateurs.values()).allSatisfy(indicateur -> assertThat(indicateur.value()).isNotNull());
        }
    }

    // ===== fabriques =====

    private CustomReportMetricDTO indicateur(String code) {
        return service.generateMetrics(List.of(code), DEBUT, FIN).get(code);
    }

    private static SalesSummaryProjection resume(long caTotal, int transactions, long marge) {
        return new SalesSummaryProjection(caTotal, transactions, 0, marge);
    }

    private static DailyCATrendProjection jour(LocalDate date, long caTotal) {
        return new DailyCATrendProjection(date, caTotal, 1);
    }

    private static TopProductProjection produit(String nom, long montant) {
        return new TopProductProjection(1L, nom, "CIP", montant, 10, 1);
    }

    private static PaymentMethodProjection paiement(String libelle, long montant) {
        return new PaymentMethodProjection(libelle, libelle, montant);
    }
}
