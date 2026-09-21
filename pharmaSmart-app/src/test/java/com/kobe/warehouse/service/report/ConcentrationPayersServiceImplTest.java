package com.kobe.warehouse.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.repository.ConcentrationPayersRepository;
import com.kobe.warehouse.service.dto.report.ConcentrationEvolutionDTO;
import com.kobe.warehouse.service.dto.report.ConcentrationEvolutionDTO.ConcentrationEvolutionSerieDTO;
import com.kobe.warehouse.service.dto.report.ConcentrationOrganismeDTO;
import com.kobe.warehouse.service.dto.report.ConcentrationSummaryDTO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * La concentration des payeurs mesure une dépendance : quelle part du tiers payant tient à un seul
 * organisme, et que se passerait-il s'il cessait de régler.
 *
 * <p>Elle se résume par l'indice de Herfindahl — la somme des carrés des parts de marché — qui ne
 * dit pas la même chose qu'une simple part du premier : dix payeurs à 10 % et un payeur à 60 %
 * suivi de quatre à 10 % donnent le même chiffre d'affaires, mais pas du tout le même risque. Le
 * seuil de l'indice décide de la couleur affichée en tête d'écran, c'est-à-dire du message que
 * l'officine retient.
 */
@DisplayName("ConcentrationPayersService — concentration des tiers payants")
class ConcentrationPayersServiceImplTest {

    // Doublure créée à l'initialisation du champ, et non dans un @BeforeEach : le délai de quinze
    // secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final ConcentrationPayersRepository repository = mock(ConcentrationPayersRepository.class);

    private final ConcentrationPayersService service = new ConcentrationPayersServiceImpl(repository);

    @BeforeEach
    void aucunTiersPayant() {
        when(repository.findConcentration(any(), any(), anyInt())).thenReturn(List.of());
        when(repository.findEvolution(anyInt())).thenReturn(List.of());
    }

    // ===== période analysée =====

    @Nested
    @DisplayName("Période analysée")
    class PeriodeAnalysee {

        /** Quatre-vingt-dix jours glissants : l'horizon par défaut de l'écran. */
        @Test
        @DisplayName("par défaut, la période couvre les quatre-vingt-dix derniers jours")
        void quatreVingtDixJours() {
            service.getSummary("90j", 10);

            assertThat(bornes()).containsExactly(LocalDate.now().minusDays(89), LocalDate.now());
        }

        @Test
        @DisplayName("la période annuelle part du premier janvier")
        void depuisLePremierJanvier() {
            service.getSummary("year", 10);

            assertThat(bornes()).containsExactly(LocalDate.now().withDayOfYear(1), LocalDate.now());
        }

        @Test
        @DisplayName("le nombre d'organismes demandé est transmis")
        void nombreDOrganismes() {
            service.getSummary("year", 5);

            verify(repository).findConcentration(any(), any(), org.mockito.ArgumentMatchers.eq(5));
        }

        private List<LocalDate> bornes() {
            ArgumentCaptor<LocalDate> debut = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> fin = ArgumentCaptor.forClass(LocalDate.class);
            verify(repository).findConcentration(debut.capture(), fin.capture(), anyInt());
            return List.of(debut.getValue(), fin.getValue());
        }
    }

    // ===== composition =====

    @Nested
    @DisplayName("Composition du tableau")
    class CompositionDuTableau {

        @Test
        @DisplayName("chaque organisme porte son chiffre, ses factures, sa part et son délai")
        void contenuDeLaLigne() {
            when(repository.findConcentration(any(), any(), anyInt())).thenReturn(
                List.<Object[]>of(organisme("CNAM", 6_000_000L, 120, 45, 60.0, 10_000_000L, 7_000_000L, 3_000_000L))
            );

            ConcentrationOrganismeDTO ligne = service.getSummary("90j", 10).organismes().getFirst();

            assertThat(ligne.organisme()).isEqualTo("CNAM");
            assertThat(ligne.caTp()).isEqualTo(6_000_000L);
            assertThat(ligne.nbFactures()).isEqualTo(120);
            assertThat(ligne.delaiReglement()).isEqualTo(45);
            assertThat(ligne.partPct()).isEqualTo(60.0);
        }

        /** Les totaux sont les mêmes sur chaque ligne : la première suffit à les lire. */
        @Test
        @DisplayName("les totaux de la période accompagnent le tableau")
        void totauxDeLaPeriode() {
            when(repository.findConcentration(any(), any(), anyInt())).thenReturn(
                List.<Object[]>of(
                    organisme("CNAM", 6_000_000L, 120, 45, 60.0, 10_000_000L, 7_000_000L, 3_000_000L),
                    organisme("MUGEF", 4_000_000L, 80, 30, 40.0, 10_000_000L, 7_000_000L, 3_000_000L)
                )
            );

            ConcentrationSummaryDTO resume = service.getSummary("90j", 10);

            assertThat(resume.totalCaTp()).isEqualTo(10_000_000L);
            assertThat(resume.totalRegle()).isEqualTo(7_000_000L);
            assertThat(resume.totalImpaye()).isEqualTo(3_000_000L);
        }

        /**
         * L'impact mensuel ramène le chiffre de la période à trente jours : c'est ce que l'officine
         * cesserait d'encaisser chaque mois si l'organisme s'arrêtait.
         */
        @Test
        @DisplayName("l'impact mensuel ramène le chiffre de la période à trente jours")
        void impactMensuel() {
            when(repository.findConcentration(any(), any(), anyInt())).thenReturn(
                List.<Object[]>of(organisme("CNAM", 9_000_000L, 120, 45, 60.0, 10_000_000L, 0L, 0L))
            );

            // Quatre-vingt-dix jours de période : le tiers du chiffre revient à un mois.
            assertThat(service.getSummary("90j", 10).organismes().getFirst().stressImpact30j()).isEqualTo(3_000_000L);
        }

        @Test
        @DisplayName("un organisme sans chiffre n'a pas d'impact mensuel")
        void impactSansChiffre() {
            when(repository.findConcentration(any(), any(), anyInt())).thenReturn(
                List.<Object[]>of(organisme("DORMANT", 0L, 0, 30, 0.0, 10_000_000L, 0L, 0L))
            );

            assertThat(service.getSummary("90j", 10).organismes().getFirst().stressImpact30j()).isZero();
        }

        @Test
        @DisplayName("une part non renseignée vaut zéro")
        void partAbsente() {
            when(repository.findConcentration(any(), any(), anyInt())).thenReturn(
                List.<Object[]>of(new Object[] { "CNAM", 1L, 1, 30, null, 1L, 0L, 0L })
            );

            assertThat(service.getSummary("90j", 10).organismes().getFirst().partPct()).isZero();
        }

        @Test
        @DisplayName("la liste des organismes est celle du résumé")
        void listeDesOrganismes() {
            when(repository.findConcentration(any(), any(), anyInt())).thenReturn(
                List.<Object[]>of(organisme("CNAM", 6_000_000L, 120, 45, 60.0, 10_000_000L, 0L, 0L))
            );

            assertThat(service.getOrganismes("90j", 10))
                .extracting(ConcentrationOrganismeDTO::organisme)
                .containsExactly("CNAM");
        }

        /** Une officine sans tiers payant n'est dépendante de personne. */
        @Test
        @DisplayName("aucun tiers payant rend un résumé vide et un risque faible")
        void aucunTiersPayant() {
            ConcentrationSummaryDTO resume = service.getSummary("90j", 10);

            assertThat(resume.organismes()).isEmpty();
            assertThat(resume.totalCaTp()).isZero();
            assertThat(resume.hhiIndex()).isZero();
            assertThat(resume.riskLevel()).isEqualTo("FAIBLE");
        }
    }

    // ===== indice de concentration =====

    @Nested
    @DisplayName("Indice de concentration")
    class IndiceDeConcentration {

        @Test
        @DisplayName("l'indice somme les carrés des parts")
        void sommeDesCarres() {
            donneParts(50.0, 50.0);

            // (0,5² + 0,5²) × 10 000
            assertThat(service.getSummary("90j", 10).hhiIndex()).isEqualTo(5_000);
        }

        @Test
        @DisplayName("un payeur unique porte l'indice au maximum")
        void payeurUnique() {
            donneParts(100.0);

            assertThat(service.getSummary("90j", 10).hhiIndex()).isEqualTo(10_000);
        }

        /** Au-delà de deux mille cinq cents, l'officine dépend d'un ou deux organismes. */
        @Test
        @DisplayName("un indice élevé annonce une dépendance forte")
        void dependanceForte() {
            donneParts(50.0, 50.0);

            assertThat(service.getSummary("90j", 10).riskLevel()).isEqualTo("ELEVE");
        }

        @Test
        @DisplayName("un indice intermédiaire appelle la vigilance")
        void dependanceModeree() {
            donneParts(20.0, 20.0, 20.0, 20.0, 20.0);

            ConcentrationSummaryDTO resume = service.getSummary("90j", 10);

            assertThat(resume.hhiIndex()).isEqualTo(2_000);
            assertThat(resume.riskLevel()).isEqualTo("MODERE");
        }

        @Test
        @DisplayName("un portefeuille très réparti annonce un risque faible")
        void dependanceFaible() {
            donneParts(5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0);

            ConcentrationSummaryDTO resume = service.getSummary("90j", 10);

            assertThat(resume.hhiIndex()).isEqualTo(250);
            assertThat(resume.riskLevel()).isEqualTo("FAIBLE");
        }

        @Test
        @DisplayName("les bornes des paliers appartiennent au palier supérieur")
        void bornesDesPaliers() {
            donneParts(31.6228, 31.6228);
            assertThat(service.getSummary("90j", 10).riskLevel()).isEqualTo("MODERE");

            donneParts(50.0, 0.0);
            assertThat(service.getSummary("90j", 10).riskLevel()).isEqualTo("ELEVE");
        }

        private void donneParts(double... parts) {
            List<Object[]> rows = java.util.Arrays.stream(parts)
                .mapToObj(part -> organisme("ORG" + part, 1_000L, 1, 30, part, 1_000L, 0L, 0L))
                .toList();
            when(repository.findConcentration(any(), any(), anyInt())).thenReturn(rows);
        }
    }

    // ===== évolution =====

    @Nested
    @DisplayName("Évolution mensuelle")
    class EvolutionMensuelle {

        @Test
        @DisplayName("chaque mois donne un point de l'axe, sans doublon")
        void axeDesMois() {
            when(repository.findEvolution(anyInt())).thenReturn(
                List.<Object[]>of(
                    mois(2026, 1, "CNAM", 5_000_000L),
                    mois(2026, 1, "MUGEF", 2_000_000L),
                    mois(2026, 2, "CNAM", 6_000_000L),
                    mois(2026, 2, "MUGEF", 2_500_000L)
                )
            );

            assertThat(service.getEvolution(5).labels()).containsExactly(libelle(2026, 1), libelle(2026, 2));
        }

        /** Un organisme sans facture un mois vaut zéro, il ne décale pas sa courbe. */
        @Test
        @DisplayName("un organisme absent un mois garde ses points alignés")
        void organismeAbsentUnMois() {
            when(repository.findEvolution(anyInt())).thenReturn(
                List.<Object[]>of(
                    mois(2026, 1, "CNAM", 5_000_000L),
                    mois(2026, 1, "MUGEF", 2_000_000L),
                    mois(2026, 2, "CNAM", 6_000_000L),
                    mois(2026, 3, "CNAM", 7_000_000L),
                    mois(2026, 3, "MUGEF", 2_500_000L)
                )
            );

            ConcentrationEvolutionDTO evolution = service.getEvolution(5);
            ConcentrationEvolutionSerieDTO mugef = evolution
                .series()
                .stream()
                .filter(s -> "MUGEF".equals(s.organisme()))
                .findFirst()
                .orElseThrow();

            assertThat(mugef.caValues()).containsExactly(2_000_000L, 0L, 2_500_000L);
        }

        @Test
        @DisplayName("toutes les séries ont autant de points que l'axe a de mois")
        void seriesDeMemeLongueur() {
            when(repository.findEvolution(anyInt())).thenReturn(
                List.<Object[]>of(mois(2026, 1, "CNAM", 1L), mois(2026, 2, "MUGEF", 2L), mois(2026, 3, "CMU", 3L))
            );

            ConcentrationEvolutionDTO evolution = service.getEvolution(5);

            assertThat(evolution.series()).allSatisfy(serie -> assertThat(serie.caValues()).hasSameSizeAs(evolution.labels()));
        }

        @Test
        @DisplayName("aucune donnée rend un graphique vide")
        void aucuneDonnee() {
            ConcentrationEvolutionDTO evolution = service.getEvolution(5);

            assertThat(evolution.labels()).isEmpty();
            assertThat(evolution.series()).isEmpty();
        }
    }

    // ===== utilitaires =====

    private static Object[] organisme(
        String nom,
        long caTp,
        int nbFactures,
        int delai,
        double partPct,
        long totalCaTp,
        long totalRegle,
        long totalImpaye
    ) {
        return new Object[] { nom, caTp, nbFactures, delai, partPct, totalCaTp, totalRegle, totalImpaye };
    }

    private static Object[] mois(int annee, int mois, String organisme, long ca) {
        return new Object[] { annee, mois, organisme, ca };
    }

    private static String libelle(int annee, int mois) {
        return YearMonth.of(annee, mois).atDay(1).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH));
    }
}
