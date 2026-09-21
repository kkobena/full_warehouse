package com.kobe.warehouse.service.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.repository.MobilePerformanceRepository;
import com.kobe.warehouse.repository.MobilePerformanceRepository.DataPointProjection;
import com.kobe.warehouse.repository.MobilePerformanceRepository.PaymentMethodProjection;
import com.kobe.warehouse.repository.MobilePerformanceRepository.PeriodSummaryProjection;
import com.kobe.warehouse.repository.MobilePerformanceRepository.TopProductProjection;
import com.kobe.warehouse.service.dto.mobile.MobilePerformanceDTO;
import com.kobe.warehouse.service.dto.mobile.MobilePerformanceDTO.PaymentMethodSummaryDTO;
import com.kobe.warehouse.service.dto.mobile.MobilePerformanceDTO.PeriodDataPointDTO;
import com.kobe.warehouse.service.dto.mobile.MobilePerformanceDTO.TopProductPerformanceDTO;
import com.kobe.warehouse.service.dto.mobile.PerformancePeriod;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * L'écran de performance répond à « comment va l'officine ce mois-ci ? ». Il le fait par deux
 * comparaisons qui ne disent pas la même chose : la période précédente, qui mesure l'effet d'une
 * action récente, et la <b>même période l'an passé</b>, qui est la lecture sur laquelle une officine
 * pilote — l'activité y est trop saisonnière pour qu'un septembre se compare utilement à un août.
 *
 * <p>Les deux ne valent que si les ensembles comparés ont la même taille. Une période en cours se
 * compare donc au <b>même avancement</b> de la période de référence : onze jours de mars contre les
 * onze premiers jours de février, et non contre février entier. Sans cela, la variation affichée est
 * négative de soixante pour cent tous les mois et ne redevient juste que le dernier jour.
 */
@DisplayName("MobilePerformanceService — performance par période")
class MobilePerformanceServiceTest {

    /** Un mercredi, volontairement en milieu de semaine et de mois. */
    private static final LocalDate MERCREDI = LocalDate.of(2026, 3, 11);

    // Doublure créée à l'initialisation du champ, et non dans un @BeforeEach : le délai de quinze
    // secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final MobilePerformanceRepository performanceRepository = mock(MobilePerformanceRepository.class);

    private final MobilePerformanceService service = new MobilePerformanceService(performanceRepository);

    @BeforeEach
    void periodeVide() {
        when(performanceRepository.getPeriodSummary(any(), any())).thenReturn(new PeriodSummaryProjection(0L, 0, 0, 0L));
        when(performanceRepository.getPaymentMethodsSummary(any(), any())).thenReturn(List.of());
        when(performanceRepository.getTopProducts(any(), any(), any(), any(), anyInt())).thenReturn(List.of());
        when(performanceRepository.getDataPoints(any(), any(), any())).thenReturn(List.of());
    }

    // ===== découpage des périodes =====

    @Nested
    @DisplayName("Découpage des périodes")
    class DecoupageDesPeriodes {

        /** La semaine court du lundi au dimanche, quel que soit le jour de consultation. */
        @Test
        @DisplayName("la semaine va du lundi au dimanche qui encadrent la date")
        void semaine() {
            MobilePerformanceDTO resultat = service.getPerformance("WEEK", MERCREDI);

            assertThat(resultat.startDate()).isEqualTo(LocalDate.of(2026, 3, 9));
            assertThat(resultat.endDate()).isEqualTo(LocalDate.of(2026, 3, 15));
        }

        /**
         * Le mercredi de la semaine courante ne se compare pas à une semaine entière : la référence
         * s'arrête au mercredi précédent, soit trois jours contre trois jours.
         */
        @Test
        @DisplayName("la semaine se compare à la précédente, arrêtée au même jour")
        void semainePrecedente() {
            service.getPerformance("WEEK", MERCREDI);

            verify(performanceRepository).getPeriodSummary(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 4));
        }

        @Test
        @DisplayName("une semaine achevée se compare à la précédente entière")
        void semaineAchevee() {
            // Dimanche 15 mars 2026 : la semaine est complète.
            service.getPerformance("WEEK", LocalDate.of(2026, 3, 15));

            verify(performanceRepository).getPeriodSummary(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8));
        }

        @Test
        @DisplayName("le mois va du premier au dernier jour")
        void mois() {
            MobilePerformanceDTO resultat = service.getPerformance("MONTH", MERCREDI);

            assertThat(resultat.startDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(resultat.endDate()).isEqualTo(LocalDate.of(2026, 3, 31));
        }

        /**
         * Le défaut que ce test fixe : onze jours de mars étaient confrontés aux vingt-huit de
         * février, ce qui rendait la variation négative d'environ soixante pour cent tous les mois,
         * quoi que fasse l'officine. La référence s'arrête au même quantième.
         */
        @Test
        @DisplayName("le mois se compare au précédent, arrêté au même quantième")
        void moisPrecedentAuMemeQuantieme() {
            service.getPerformance("MONTH", MERCREDI);

            verify(performanceRepository).getPeriodSummary(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 11));
        }

        @Test
        @DisplayName("un mois achevé se compare au précédent entier")
        void moisAcheve() {
            service.getPerformance("MONTH", LocalDate.of(2026, 3, 31));

            verify(performanceRepository).getPeriodSummary(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        }

        /** Le 31 mars n'existe pas en février : le plafond retient le dernier jour du mois. */
        @Test
        @DisplayName("un quantième absent du mois de référence retient son dernier jour")
        void quantiemeAbsent() {
            service.getPerformance("MONTH", LocalDate.of(2026, 3, 30));

            verify(performanceRepository).getPeriodSummary(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        }

        @Test
        @DisplayName("l'année va du premier janvier au trente-et-un décembre")
        void annee() {
            MobilePerformanceDTO resultat = service.getPerformance("YEAR", MERCREDI);

            assertThat(resultat.startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(resultat.endDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        }

        /** Sur une année, période précédente et an passé désignent le même intervalle : il est interrogé deux fois. */
        @Test
        @DisplayName("l'année se compare à la précédente, arrêtée à la même date")
        void anneePrecedente() {
            service.getPerformance("YEAR", MERCREDI);

            verify(performanceRepository, org.mockito.Mockito.times(2))
                .getPeriodSummary(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 11));
        }

        @Test
        @DisplayName("une année achevée se compare à la précédente entière")
        void anneeAchevee() {
            service.getPerformance("YEAR", LocalDate.of(2026, 12, 31));

            verify(performanceRepository, org.mockito.Mockito.times(2))
                .getPeriodSummary(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        }

        @Test
        @DisplayName("la période demandée est reportée dans le résultat")
        void periodeReportee() {
            assertThat(service.getPerformance("month", MERCREDI).period()).isEqualTo("MONTH");
        }

        @Test
        @DisplayName("la période se demande indifféremment par son nom ou par sa valeur")
        void parEnumeration() {
            assertThat(service.getPerformance(PerformancePeriod.WEEK, MERCREDI).period()).isEqualTo("WEEK");
        }

        @Test
        @DisplayName("une période inconnue est refusée franchement")
        void periodeInconnue() {
            assertThatThrownBy(() -> service.getPerformance("TRIMESTRE", MERCREDI))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TRIMESTRE");
        }

        @Test
        @DisplayName("une période absente est refusée franchement")
        void periodeAbsente() {
            assertThatThrownBy(() -> service.getPerformance((String) null, MERCREDI))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ===== comparaison annuelle =====

    @Nested
    @DisplayName("Comparaison à l'an passé")
    class ComparaisonALAnPasse {

        /**
         * C'est la lecture de tête : elle met en regard deux périodes de même nature, là où la
         * comparaison au mois précédent est dominée par la saison — épidémies, saison des pluies,
         * rentrée, congés.
         */
        @Test
        @DisplayName("le mois se compare au même mois de l'an passé, au même quantième")
        void memeMoisAnPasse() {
            service.getPerformance("MONTH", MERCREDI);

            // Mars de l'an passé, arrêté au 11 : le même mois, non le mois précédent.
            verify(performanceRepository).getPeriodSummary(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 11));
        }

        @Test
        @DisplayName("la semaine se compare à la même semaine de l'an passé")
        void memeSemaineAnPasse() {
            service.getPerformance("WEEK", MERCREDI);

            // Le lundi de la semaine courante, un an plus tôt, et le même avancement de trois jours.
            verify(performanceRepository).getPeriodSummary(LocalDate.of(2025, 3, 9), LocalDate.of(2025, 3, 11));
        }

        @Test
        @DisplayName("l'écart à l'an passé accompagne le chiffre de l'an passé")
        void ecartALAnPasse() {
            when(performanceRepository.getPeriodSummary(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(new PeriodSummaryProjection(1_200_000L, 1, 1, 0L));
            when(performanceRepository.getPeriodSummary(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 11)))
                .thenReturn(new PeriodSummaryProjection(1_000_000L, 1, 1, 0L));

            MobilePerformanceDTO resultat = service.getPerformance("MONTH", MERCREDI);

            assertThat(resultat.caSamePeriodLastYear()).isEqualTo(1_000_000L);
            assertThat(resultat.variationVsLastYearPercent()).isEqualTo(20.0);
        }

        /**
         * Une officine ouverte depuis six mois n'a pas progressé de cent pour cent : elle n'a rien à
         * quoi se comparer. Le service le dit en ne rendant aucune variation, et l'écran affiche un
         * tiret — là où « +100 % vs l'an passé » s'affichait toute la première année, en vert,
         * exactement comme une vraie croissance.
         */
        @Test
        @DisplayName("sans historique annuel, aucune variation n'est rendue")
        void sansHistorique() {
            when(performanceRepository.getPeriodSummary(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(new PeriodSummaryProjection(500_000L, 1, 1, 0L));

            MobilePerformanceDTO resultat = service.getPerformance("MONTH", MERCREDI);

            assertThat(resultat.variationVsLastYearPercent()).isNull();
            // Le chiffre de l'an passé reste à zéro : c'est une donnée, non une comparaison.
            assertThat(resultat.caSamePeriodLastYear()).isZero();
        }

        /** L'écran doit pouvoir dire ce qu'il compare : les bornes de la référence l'accompagnent. */
        @Test
        @DisplayName("les bornes de la période de référence sont rendues")
        void bornesDeLaReference() {
            MobilePerformanceDTO resultat = service.getPerformance("MONTH", MERCREDI);

            assertThat(resultat.previousStartDate()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(resultat.previousEndDate()).isEqualTo(LocalDate.of(2026, 2, 11));
        }

        /** Sur une année, la période précédente et l'an passé désignent le même intervalle. */
        @Test
        @DisplayName("sur une année, les deux comparaisons coïncident")
        void anneeLesDeuxCoincident() {
            when(performanceRepository.getPeriodSummary(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 11)))
                .thenReturn(new PeriodSummaryProjection(800_000L, 1, 1, 0L));

            MobilePerformanceDTO resultat = service.getPerformance("YEAR", MERCREDI);

            assertThat(resultat.caSamePeriodLastYear()).isEqualTo(resultat.caPreviousPeriod());
        }
    }

    // ===== indicateurs =====

    @Nested
    @DisplayName("Indicateurs de la période")
    class Indicateurs {

        @Test
        @DisplayName("les chiffres de la période sont repris tels quels")
        void chiffresRepris() {
            donnePeriode(new PeriodSummaryProjection(1_000_000L, 40, 35, 300_000L));

            MobilePerformanceDTO resultat = service.getPerformance("MONTH", MERCREDI);

            assertThat(resultat.caTotal()).isEqualTo(1_000_000L);
            assertThat(resultat.transactionsCount()).isEqualTo(40);
            assertThat(resultat.customersCount()).isEqualTo(35);
            assertThat(resultat.marginTotal()).isEqualTo(300_000L);
        }

        @Test
        @DisplayName("le panier moyen se déduit du chiffre d'affaires et du nombre de tickets")
        void panierMoyen() {
            donnePeriode(new PeriodSummaryProjection(1_000_000L, 40, 35, 0L));

            assertThat(service.getPerformance("MONTH", MERCREDI).averageBasket()).isEqualTo(25_000L);
        }

        /** Une période sans ticket ne doit pas faire diviser par zéro. */
        @Test
        @DisplayName("une période sans ticket rend un panier moyen nul")
        void panierMoyenSansTicket() {
            assertThat(service.getPerformance("MONTH", MERCREDI).averageBasket()).isZero();
        }

        @Test
        @DisplayName("le taux de marge se calcule sur le chiffre d'affaires, arrondi au centième")
        void tauxDeMarge() {
            donnePeriode(new PeriodSummaryProjection(3_000_000L, 1, 1, 1_000_000L));

            assertThat(service.getPerformance("MONTH", MERCREDI).marginPercent()).isEqualTo(33.33);
        }

        @Test
        @DisplayName("une période sans chiffre d'affaires rend un taux de marge nul")
        void tauxDeMargeSansChiffreDAffaires() {
            assertThat(service.getPerformance("MONTH", MERCREDI).marginPercent()).isZero();
        }

        @Test
        @DisplayName("la variation confronte la période à la précédente")
        void variation() {
            donnePeriode(new PeriodSummaryProjection(1_200_000L, 1, 1, 0L));
            when(performanceRepository.getPeriodSummary(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 11)))
                .thenReturn(new PeriodSummaryProjection(1_000_000L, 1, 1, 0L));

            MobilePerformanceDTO resultat = service.getPerformance("MONTH", MERCREDI);

            assertThat(resultat.caPreviousPeriod()).isEqualTo(1_000_000L);
            assertThat(resultat.variationPercent()).isEqualTo(20.0);
        }

        @Test
        @DisplayName("une période précédente vide ne rend aucune variation")
        void periodePrecedenteVide() {
            donnePeriode(new PeriodSummaryProjection(500_000L, 1, 1, 0L));

            assertThat(service.getPerformance("MONTH", MERCREDI).variationPercent()).isNull();
        }

        /** Deux périodes sans activité se valent : la variation est nulle, non absente. */
        @Test
        @DisplayName("deux périodes vides donnent une variation de zéro")
        void deuxPeriodesVides() {
            assertThat(service.getPerformance("MONTH", MERCREDI).variationPercent()).isEqualTo(0.0);
        }
    }

    // ===== modes de règlement =====

    @Nested
    @DisplayName("Modes de règlement")
    class ModesDeReglement {

        @Test
        @DisplayName("chaque mode porte sa part du chiffre d'affaires, arrondie au dixième")
        void partDuChiffreDAffaires() {
            donnePeriode(new PeriodSummaryProjection(1_000_000L, 40, 35, 0L));
            when(performanceRepository.getPaymentMethodsSummary(any(), any())).thenReturn(
                List.of(new PaymentMethodProjection("CASH", "Espèces", 750_000L, 30))
            );

            PaymentMethodSummaryDTO mode = service.getPerformance("MONTH", MERCREDI).paymentMethods().getFirst();

            assertThat(mode.code()).isEqualTo("CASH");
            assertThat(mode.label()).isEqualTo("Espèces");
            assertThat(mode.amount()).isEqualTo(750_000L);
            assertThat(mode.percent()).isEqualTo(75.0);
            assertThat(mode.transactionsCount()).isEqualTo(30);
        }

        /**
         * Le défaut que ce test fixe : la couleur se cherchait sur le nom du <i>groupe</i> de
         * règlement, alors que la requête remonte le <i>code du mode</i>. Chèques et mobile money
         * tombaient donc tous sur le gris par défaut, indistinguables les uns des autres.
         */
        @Test
        @DisplayName("chaque mode reçoit la couleur de son groupe de règlement")
        void couleurParGroupe() {
            when(performanceRepository.getPaymentMethodsSummary(any(), any())).thenReturn(
                List.of(
                    new PaymentMethodProjection("CASH", "Espèces", 1L, 1),
                    new PaymentMethodProjection("CB", "Carte", 1L, 1),
                    new PaymentMethodProjection("OM", "Orange Money", 1L, 1),
                    new PaymentMethodProjection("WAVE", "Wave", 1L, 1),
                    new PaymentMethodProjection("CH", "Chèque", 1L, 1),
                    new PaymentMethodProjection("VIREMENT", "Virement", 1L, 1)
                )
            );

            assertThat(service.getPerformance("MONTH", MERCREDI).paymentMethods())
                .extracting(PaymentMethodSummaryDTO::color)
                .containsExactly("#28A745", "#007BFF", "#FFC107", "#FFC107", "#6C757D", "#17A2B8");
        }

        @Test
        @DisplayName("un code de règlement inconnu reçoit la couleur neutre")
        void codeInconnu() {
            when(performanceRepository.getPaymentMethodsSummary(any(), any())).thenReturn(
                List.of(new PaymentMethodProjection("TROC", "Troc", 1L, 1))
            );

            assertThat(service.getPerformance("MONTH", MERCREDI).paymentMethods().getFirst().color())
                .isEqualTo("#6C757D");
        }

        @Test
        @DisplayName("une période sans chiffre d'affaires rend des parts nulles")
        void partsNulles() {
            when(performanceRepository.getPaymentMethodsSummary(any(), any())).thenReturn(
                List.of(new PaymentMethodProjection("CASH", "Espèces", 1_000L, 1))
            );

            assertThat(service.getPerformance("MONTH", MERCREDI).paymentMethods().getFirst().percent()).isZero();
        }
    }

    // ===== palmarès et courbe =====

    @Nested
    @DisplayName("Palmarès et courbe")
    class PalmaresEtCourbe {

        @Test
        @DisplayName("le palmarès numérote ses lignes dans l'ordre reçu")
        void numerotation() {
            when(performanceRepository.getTopProducts(any(), any(), any(), any(), anyInt())).thenReturn(
                List.of(produit("DOLIPRANE", 500_000L, 50.0, 12.345), produit("EFFERALGAN", 300_000L, 30.0, -8.0))
            );

            List<TopProductPerformanceDTO> palmares = service.getPerformance("MONTH", MERCREDI).topProducts();

            assertThat(palmares).extracting(TopProductPerformanceDTO::rank).containsExactly(1, 2);
            assertThat(palmares.getFirst().productName()).isEqualTo("DOLIPRANE");
        }

        @Test
        @DisplayName("part et variation du produit sont arrondies au dixième")
        void arrondisDuPalmares() {
            when(performanceRepository.getTopProducts(any(), any(), any(), any(), anyInt())).thenReturn(
                List.of(produit("DOLIPRANE", 500_000L, 33.333, 12.345))
            );

            TopProductPerformanceDTO produit = service.getPerformance("MONTH", MERCREDI).topProducts().getFirst();

            assertThat(produit.percentOfTotal()).isEqualTo(33.3);
            assertThat(produit.variationPercent()).isEqualTo(12.3);
        }

        @Test
        @DisplayName("le palmarès se limite à dix produits")
        void dixProduits() {
            service.getPerformance("MONTH", MERCREDI);

            verify(performanceRepository).getTopProducts(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 11),
                10
            );
        }

        /** Semaine et mois se lisent au jour ; l'année, elle, se lit au mois. */
        @Test
        @DisplayName("la courbe d'une semaine porte les jours, celle d'une année les mois")
        void libellesDeLaCourbe() {
            // Le 9 mars 2026 est un lundi.
            when(performanceRepository.getDataPoints(any(), any(), any())).thenReturn(
                List.of(new DataPointProjection(LocalDate.of(2026, 3, 9), 800_000L, 30, 200_000L))
            );

            assertThat(service.getPerformance("WEEK", MERCREDI).dataPoints().getFirst().label()).startsWith("lun");
            assertThat(service.getPerformance("YEAR", MERCREDI).dataPoints().getFirst().label()).startsWith("mars");
        }

        @Test
        @DisplayName("chaque point de la courbe porte son chiffre d'affaires et sa marge")
        void contenuDeLaCourbe() {
            when(performanceRepository.getDataPoints(any(), any(), any())).thenReturn(
                List.of(new DataPointProjection(LocalDate.of(2026, 3, 9), 800_000L, 30, 200_000L))
            );

            PeriodDataPointDTO point = service.getPerformance("WEEK", MERCREDI).dataPoints().getFirst();

            assertThat(point.date()).isEqualTo(LocalDate.of(2026, 3, 9));
            assertThat(point.caAmount()).isEqualTo(800_000L);
            assertThat(point.transactionsCount()).isEqualTo(30);
            assertThat(point.marginAmount()).isEqualTo(200_000L);
        }

        @Test
        @DisplayName("la courbe est demandée pour la période, avec son découpage")
        void decoupageDeLaCourbe() {
            service.getPerformance("YEAR", MERCREDI);

            verify(performanceRepository).getDataPoints(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                PerformancePeriod.YEAR
            );
        }

        @Test
        @DisplayName("une période sans vente rend un palmarès et une courbe vides")
        void periodeSansVente() {
            MobilePerformanceDTO resultat = service.getPerformance("MONTH", MERCREDI);

            assertThat(resultat.topProducts()).isEmpty();
            assertThat(resultat.dataPoints()).isEmpty();
            assertThat(resultat.paymentMethods()).isEmpty();
        }
    }

    // ===== utilitaires =====

    /** Renseigne la période courante de mars sans toucher à celle de février. */
    private void donnePeriode(PeriodSummaryProjection resume) {
        when(performanceRepository.getPeriodSummary(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).thenReturn(resume);
    }

    private static TopProductProjection produit(String libelle, long montant, double part, double variation) {
        return new TopProductProjection(1L, libelle, "CIP", montant, 10, part, variation);
    }
}
