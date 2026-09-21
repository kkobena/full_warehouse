package com.kobe.warehouse.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.repository.PnlAnalytiqueRepository;
import com.kobe.warehouse.service.dto.report.PnlEvolutionDTO;
import com.kobe.warehouse.service.dto.report.PnlEvolutionDTO.PnlEvolutionSerieDTO;
import com.kobe.warehouse.service.dto.report.PnlFamilleDTO;
import com.kobe.warehouse.service.dto.report.PnlSegmentDTO;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Le compte de résultat analytique répond à « où gagne-t-on sa marge ? » : par segment de vente, par
 * famille de produits, et comment cela évolue de mois en mois.
 *
 * <p>Les courbes d'évolution sont le point délicat. Elles se construisent à partir de lignes
 * {@code (mois, série, taux)} que la base ne produit <b>que si la série a vendu ce mois-là</b> :
 * il n'y a pas de ligne à zéro pour une famille absente. Empiler les valeurs dans leur ordre
 * d'arrivée décalait alors toute la fin de la courbe d'un cran — le taux de mars s'affichant sur
 * février — sans que rien ne le signale, puisque la courbe restait parfaitement lisible.
 */
@DisplayName("PnlAnalytiqueService — compte de résultat analytique")
class PnlAnalytiqueServiceImplTest {

    // Doublure créée à l'initialisation du champ, et non dans un @BeforeEach : le délai de quinze
    // secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final PnlAnalytiqueRepository repository = mock(PnlAnalytiqueRepository.class);

    private final PnlAnalytiqueService service = new PnlAnalytiqueServiceImpl(repository);

    // ===== instantané par segment =====

    @Nested
    @DisplayName("Instantané par segment")
    class InstantaneParSegment {

        @Test
        @DisplayName("chaque segment porte ses montants et son taux de marge")
        void montantsDuSegment() {
            when(repository.findSnapshotBySegment(2026)).thenReturn(
                List.<Object[]>of(new Object[] { "COMPTANT", 1_000_000L, 600_000L, 400_000L, "40.00", 250 })
            );

            PnlSegmentDTO segment = service.getSnapshotBySegment(2026).getFirst();

            assertThat(segment.segment()).isEqualTo("COMPTANT");
            assertThat(segment.ca()).isEqualTo(1_000_000L);
            assertThat(segment.coutAchat()).isEqualTo(600_000L);
            assertThat(segment.margeBrute()).isEqualTo(400_000L);
            assertThat(segment.tauxMarge()).isEqualByComparingTo("40.00");
            assertThat(segment.nbTransactions()).isEqualTo(250);
        }

        /** La nature de vente est un code technique : l'écran attend un libellé d'officine. */
        @Test
        @DisplayName("la nature de vente se traduit en libellé lisible")
        void libelleDuSegment() {
            when(repository.findSnapshotBySegment(2026)).thenReturn(
                List.<Object[]>of(
                    new Object[] { "COMPTANT", 1L, 0L, 1L, "1", 1 },
                    new Object[] { "ASSURANCE", 1L, 0L, 1L, "1", 1 },
                    new Object[] { "CARNET", 1L, 0L, 1L, "1", 1 }
                )
            );

            assertThat(service.getSnapshotBySegment(2026))
                .extracting(PnlSegmentDTO::segmentLabel)
                .containsExactly("Comptant", "Ordonnancée (tiers payant)", "Carnet");
        }

        @Test
        @DisplayName("une nature inconnue s'affiche telle quelle plutôt que d'interrompre le rapport")
        void natureInconnue() {
            when(repository.findSnapshotBySegment(2026)).thenReturn(
                List.<Object[]>of(new Object[] { "TROC", 1L, 0L, 1L, "1", 1 })
            );

            assertThat(service.getSnapshotBySegment(2026).getFirst().segmentLabel()).isEqualTo("TROC");
        }

        @Test
        @DisplayName("une nature absente se range dans « Autre »")
        void natureAbsente() {
            when(repository.findSnapshotBySegment(2026)).thenReturn(
                List.<Object[]>of(new Object[] { null, 1L, 0L, 1L, "1", 1 })
            );

            assertThat(service.getSnapshotBySegment(2026).getFirst().segmentLabel()).isEqualTo("Autre");
        }

        @Test
        @DisplayName("un montant non renseigné vaut zéro")
        void montantAbsent() {
            when(repository.findSnapshotBySegment(2026)).thenReturn(
                List.<Object[]>of(new Object[] { "COMPTANT", null, null, null, null, null })
            );

            PnlSegmentDTO segment = service.getSnapshotBySegment(2026).getFirst();

            assertThat(segment.ca()).isZero();
            assertThat(segment.tauxMarge()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(segment.nbTransactions()).isZero();
        }

        @Test
        @DisplayName("une année sans vente rend une liste vide")
        void anneeSansVente() {
            when(repository.findSnapshotBySegment(2026)).thenReturn(List.of());

            assertThat(service.getSnapshotBySegment(2026)).isEmpty();
        }
    }

    // ===== instantané par famille =====

    @Nested
    @DisplayName("Instantané par famille")
    class InstantaneParFamille {

        @Test
        @DisplayName("chaque famille porte ses montants et son taux de marge")
        void montantsDeLaFamille() {
            when(repository.findSnapshotByFamille(2026)).thenReturn(
                List.<Object[]>of(new Object[] { "ANTALGIQUES", 800_000L, 500_000L, 300_000L, "37.50" })
            );

            PnlFamilleDTO famille = service.getSnapshotByFamille(2026).getFirst();

            assertThat(famille.famille()).isEqualTo("ANTALGIQUES");
            assertThat(famille.ca()).isEqualTo(800_000L);
            assertThat(famille.coutAchat()).isEqualTo(500_000L);
            assertThat(famille.margeBrute()).isEqualTo(300_000L);
            assertThat(famille.tauxMarge()).isEqualByComparingTo("37.50");
        }

        @Test
        @DisplayName("l'ordre rendu par la base est conservé")
        void ordreConserve() {
            when(repository.findSnapshotByFamille(2026)).thenReturn(
                List.<Object[]>of(
                    new Object[] { "ANTALGIQUES", 800_000L, 0L, 0L, "0" },
                    new Object[] { "ANTIBIOTIQUES", 500_000L, 0L, 0L, "0" }
                )
            );

            assertThat(service.getSnapshotByFamille(2026))
                .extracting(PnlFamilleDTO::famille)
                .containsExactly("ANTALGIQUES", "ANTIBIOTIQUES");
        }
    }

    // ===== évolution =====

    @Nested
    @DisplayName("Évolution mensuelle par famille")
    class EvolutionParFamille {

        @Test
        @DisplayName("chaque mois donne un point de l'axe, sans doublon")
        void axeDesMois() {
            when(repository.findEvolutionMonthlyByFamille()).thenReturn(
                List.<Object[]>of(
                    mois(2026, 1, "ANTALGIQUES", "40.00"),
                    mois(2026, 1, "ANTIBIOTIQUES", "30.00"),
                    mois(2026, 2, "ANTALGIQUES", "42.00"),
                    mois(2026, 2, "ANTIBIOTIQUES", "31.00")
                )
            );

            assertThat(service.getEvolutionByFamille().labels()).containsExactly(libelle(2026, 1), libelle(2026, 2));
        }

        /**
         * Le défaut que ce test fixe : la base ne produit pas de ligne pour une famille qui n'a rien
         * vendu. Empiler les valeurs reçues faisait alors glisser toute la fin de la courbe d'un
         * cran, et le taux de mars s'affichait sur février.
         */
        @Test
        @DisplayName("une famille absente un mois garde ses points alignés sur les bons mois")
        void familleAbsenteUnMois() {
            when(repository.findEvolutionMonthlyByFamille()).thenReturn(
                List.<Object[]>of(
                    mois(2026, 1, "ANTALGIQUES", "40.00"),
                    mois(2026, 1, "ANTIBIOTIQUES", "30.00"),
                    // Février : les antibiotiques n'ont rien vendu, ils n'ont pas de ligne.
                    mois(2026, 2, "ANTALGIQUES", "42.00"),
                    mois(2026, 3, "ANTALGIQUES", "44.00"),
                    mois(2026, 3, "ANTIBIOTIQUES", "33.00")
                )
            );

            PnlEvolutionDTO evolution = service.getEvolutionByFamille();
            PnlEvolutionSerieDTO antibiotiques = serie(evolution, "ANTIBIOTIQUES");

            assertThat(evolution.labels()).hasSize(3);
            assertThat(antibiotiques.tauxMargeValues()).hasSize(3);
            assertThat(antibiotiques.tauxMargeValues().get(0)).isEqualByComparingTo("30.00");
            assertThat(antibiotiques.tauxMargeValues().get(1)).isEqualByComparingTo("0");
            assertThat(antibiotiques.tauxMargeValues().get(2)).isEqualByComparingTo("33.00");
        }

        @Test
        @DisplayName("toutes les séries ont autant de points que l'axe a de mois")
        void seriesDeMemeLongueur() {
            when(repository.findEvolutionMonthlyByFamille()).thenReturn(
                List.<Object[]>of(
                    mois(2026, 1, "ANTALGIQUES", "40.00"),
                    mois(2026, 2, "ANTIBIOTIQUES", "30.00"),
                    mois(2026, 3, "VITAMINES", "20.00")
                )
            );

            PnlEvolutionDTO evolution = service.getEvolutionByFamille();

            assertThat(evolution.series())
                .allSatisfy(serie -> assertThat(serie.tauxMargeValues()).hasSameSizeAs(evolution.labels()));
        }

        @Test
        @DisplayName("la série porte le nom de la famille et non celui d'un segment")
        void serieNommeeParFamille() {
            when(repository.findEvolutionMonthlyByFamille()).thenReturn(
                List.<Object[]>of(mois(2026, 1, "ANTALGIQUES", "40.00"))
            );

            PnlEvolutionSerieDTO serie = service.getEvolutionByFamille().series().getFirst();

            assertThat(serie.famille()).isEqualTo("ANTALGIQUES");
            assertThat(serie.segment()).isNull();
        }

        @Test
        @DisplayName("un taux non renseigné vaut zéro")
        void tauxAbsent() {
            when(repository.findEvolutionMonthlyByFamille()).thenReturn(
                List.<Object[]>of(mois(2026, 1, "ANTALGIQUES", null))
            );

            assertThat(service.getEvolutionByFamille().series().getFirst().tauxMargeValues().getFirst())
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("aucune donnée rend un graphique vide")
        void aucuneDonnee() {
            when(repository.findEvolutionMonthlyByFamille()).thenReturn(List.of());

            PnlEvolutionDTO evolution = service.getEvolutionByFamille();

            assertThat(evolution.labels()).isEmpty();
            assertThat(evolution.series()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Évolution mensuelle par segment")
    class EvolutionParSegment {

        @Test
        @DisplayName("un segment absent un mois garde ses points alignés")
        void segmentAbsentUnMois() {
            when(repository.findEvolutionMonthlyBySegment()).thenReturn(
                List.<Object[]>of(
                    mois(2026, 1, "COMPTANT", "40.00"),
                    mois(2026, 1, "CARNET", "25.00"),
                    // Février sans vente au carnet : aucune ligne pour ce segment.
                    mois(2026, 2, "COMPTANT", "41.00"),
                    mois(2026, 3, "COMPTANT", "42.00"),
                    mois(2026, 3, "CARNET", "27.00")
                )
            );

            PnlEvolutionDTO evolution = service.getEvolutionBySegment();
            List<BigDecimal> carnet = evolution
                .series()
                .stream()
                .filter(s -> "Carnet".equals(s.segment()))
                .findFirst()
                .orElseThrow()
                .tauxMargeValues();

            assertThat(carnet).hasSize(3);
            assertThat(carnet.get(1)).isEqualByComparingTo("0");
            assertThat(carnet.get(2)).isEqualByComparingTo("27.00");
        }

        @Test
        @DisplayName("la série porte le libellé du segment et non celui d'une famille")
        void serieNommeeParSegment() {
            when(repository.findEvolutionMonthlyBySegment()).thenReturn(
                List.<Object[]>of(mois(2026, 1, "ASSURANCE", "30.00"))
            );

            PnlEvolutionSerieDTO serie = service.getEvolutionBySegment().series().getFirst();

            assertThat(serie.segment()).isEqualTo("Ordonnancée (tiers payant)");
            assertThat(serie.famille()).isNull();
        }
    }

    // ===== utilitaires =====

    private static Object[] mois(int annee, int mois, String serie, String taux) {
        return new Object[] { annee, mois, serie, taux };
    }

    private static String libelle(int annee, int mois) {
        return YearMonth.of(annee, mois)
            .atDay(1)
            .format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", java.util.Locale.FRENCH));
    }

    private static PnlEvolutionSerieDTO serie(PnlEvolutionDTO evolution, String famille) {
        return evolution.series().stream().filter(s -> famille.equals(s.famille())).findFirst().orElseThrow();
    }
}
