package com.kobe.warehouse.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.enumeration.StockAlertType;
import com.kobe.warehouse.service.dashboard.mapper.DashboardDTOMapper;
import com.kobe.warehouse.service.dto.dashboard.AnalyseABCDTO;
import com.kobe.warehouse.service.dto.dashboard.PerformanceFournisseurDTO;
import com.kobe.warehouse.service.dto.dashboard.StockAlertsDTO;
import com.kobe.warehouse.service.dto.report.ABCParetoSummaryDTO;
import com.kobe.warehouse.service.dto.report.StockAlertDTO;
import com.kobe.warehouse.service.dto.report.SupplierPerformanceDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Le mapper est la frontière entre les services de rapport, qui raisonnent en lignes détaillées, et
 * les tuiles du tableau de bord, qui ne montrent que des compteurs et des notes. Tout ce qu'il fait
 * est donc une réduction — et une réduction se trompe silencieusement : un compteur faux ressemble
 * à un compteur juste.
 *
 * <p>Trois réductions sont éprouvées ici. Le <b>comptage par type d'alerte</b> : une rupture n'est
 * pas une alerte de seuil, et les ranger dans la même case ferait croire à un stock sain. La
 * <b>conversion ABC</b>, où les pourcentages arrivent en {@code BigDecimal} possiblement nuls — un
 * {@code null} non gardé remonterait en {@code NullPointerException} jusqu'à l'écran. Enfin la
 * <b>note fournisseur sur cinq</b>, dont les paliers sont le seul endroit du code où un score de
 * performance devient une décision lisible par l'acheteur.
 */
@DisplayName("DashboardDTOMapper — réduction des rapports en tuiles de tableau de bord")
class DashboardDTOMapperTest {

    private final DashboardDTOMapper mapper = new DashboardDTOMapper();

    // ===== alertes de stock =====

    @Nested
    @DisplayName("Alertes de stock")
    class AlertesDeStock {

        @Test
        @DisplayName("compte séparément ruptures, alertes de seuil et péremptions")
        void compteChaqueTypeSeparement() {
            List<StockAlertDTO> alertes = List.of(
                alerte(1, StockAlertType.RUPTURE),
                alerte(2, StockAlertType.RUPTURE),
                alerte(3, StockAlertType.ALERTE),
                alerte(4, StockAlertType.PEREMPTION),
                alerte(5, StockAlertType.PEREMPTION),
                alerte(6, StockAlertType.PEREMPTION)
            );

            StockAlertsDTO resultat = mapper.toStockAlertsDTO(alertes);

            assertThat(resultat.rupture()).isEqualTo(2);
            assertThat(resultat.stockCritique()).isEqualTo(1);
            assertThat(resultat.reassortStockRayon()).isEqualTo(3);
        }

        /**
         * {@code bientotEnRupture} n'est pas calculable depuis {@link StockAlertDTO}, qui ne porte
         * pas le stock projeté. Le mapper renvoie donc zéro, et le test le fixe : le jour où la
         * tuile affichera autre chose, ce sera parce qu'on l'aura décidé.
         */
        @Test
        @DisplayName("laisse « bientôt en rupture » à zéro, faute de donnée source")
        void bientotEnRuptureResteAZero() {
            StockAlertsDTO resultat = mapper.toStockAlertsDTO(List.of(alerte(1, StockAlertType.RUPTURE)));

            assertThat(resultat.bientotEnRupture()).isZero();
        }

        @Test
        @DisplayName("une liste vide donne des compteurs à zéro, pas un null")
        void listeVideDonneDesZeros() {
            StockAlertsDTO resultat = mapper.toStockAlertsDTO(List.of());

            assertThat(resultat.rupture()).isZero();
            assertThat(resultat.stockCritique()).isZero();
            assertThat(resultat.reassortStockRayon()).isZero();
        }

        private StockAlertDTO alerte(int produitId, StockAlertType type) {
            return new StockAlertDTO(produitId, "PRODUIT " + produitId, "CIP" + produitId, 0, 5, LocalDate.now().plusMonths(1), type);
        }
    }

    // ===== analyse ABC =====

    @Nested
    @DisplayName("Analyse ABC")
    class AnalyseABC {

        @Test
        @DisplayName("reporte produits, pourcentage et CA de chaque classe")
        void reporteLesTroisClasses() {
            AnalyseABCDTO resultat = mapper.toAnalyseABCDTO(
                resume(BigDecimal.valueOf(78.5), BigDecimal.valueOf(16.2), BigDecimal.valueOf(5.3))
            );

            assertThat(resultat.classeA().nombreProduits()).isEqualTo(10);
            assertThat(resultat.classeA().pourcentageProduits()).isEqualTo(78.5);
            assertThat(resultat.classeA().valeur()).isEqualTo(8_000_000L);

            assertThat(resultat.classeB().nombreProduits()).isEqualTo(30);
            assertThat(resultat.classeB().pourcentageProduits()).isEqualTo(16.2);
            assertThat(resultat.classeB().valeur()).isEqualTo(1_500_000L);

            assertThat(resultat.classeC().nombreProduits()).isEqualTo(60);
            assertThat(resultat.classeC().pourcentageProduits()).isEqualTo(5.3);
            assertThat(resultat.classeC().valeur()).isEqualTo(500_000L);
        }

        /**
         * Les 80 / 15 / 5 ne viennent pas de la base : ce sont les bornes de Pareto contre
         * lesquelles l'acheteur compare la répartition réelle. Les confondre avec les pourcentages
         * mesurés priverait la tuile de son point de comparaison.
         */
        @Test
        @DisplayName("garde les bornes de Pareto 80/15/5 comme référence fixe")
        void bornesDeParetoFixes() {
            AnalyseABCDTO resultat = mapper.toAnalyseABCDTO(resume(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));

            assertThat(resultat.classeA().pourcentageCA()).isEqualTo(80.0);
            assertThat(resultat.classeB().pourcentageCA()).isEqualTo(15.0);
            assertThat(resultat.classeC().pourcentageCA()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("remplace un pourcentage absent par zéro plutôt que de propager le null")
        void pourcentageAbsentDevientZero() {
            AnalyseABCDTO resultat = mapper.toAnalyseABCDTO(resume(null, null, null));

            assertThat(resultat.classeA().pourcentageProduits()).isZero();
            assertThat(resultat.classeB().pourcentageProduits()).isZero();
            assertThat(resultat.classeC().pourcentageProduits()).isZero();
        }

        @Test
        @DisplayName("sans résumé, rend null — la tuile sait masquer ce cas")
        void resumeAbsentDonneNull() {
            assertThat(mapper.toAnalyseABCDTO(null)).isNull();
        }

        private ABCParetoSummaryDTO resume(BigDecimal pctA, BigDecimal pctB, BigDecimal pctC) {
            return new ABCParetoSummaryDTO(
                100,
                10_000_000L,
                5,
                4_000_000L,
                BigDecimal.valueOf(40),
                10,
                8_000_000L,
                pctA,
                30,
                1_500_000L,
                pctB,
                60,
                500_000L,
                pctC,
                0,
                0L,
                BigDecimal.ZERO
            );
        }
    }

    // ===== performance fournisseur =====

    @Nested
    @DisplayName("Performance fournisseur")
    class PerformanceFournisseur {

        @Test
        @DisplayName("reporte identité, volume et délais du fournisseur")
        void reporteLesIndicateurs() {
            PerformanceFournisseurDTO resultat = mapper.toPerformanceFournisseurDTO(
                fournisseur(7, 24, 3, BigDecimal.valueOf(95.5), BigDecimal.valueOf(88))
            );

            assertThat(resultat.fournisseurId()).isEqualTo(7L);
            assertThat(resultat.fournisseurName()).isEqualTo("LABOREX");
            assertThat(resultat.nombreCommandes()).isEqualTo(24);
            assertThat(resultat.delaiMoyenJours()).isEqualTo(3.0);
            assertThat(resultat.tauxConformite()).isEqualTo(95.5);
            assertThat(resultat.caAnnuel()).isEqualTo(120_000_000L);
        }

        /**
         * Les paliers sont testés sur leurs bornes exactes : c'est là, et nulle part ailleurs, que
         * se joue la différence entre trois et quatre étoiles affichées à l'acheteur.
         */
        @ParameterizedTest(name = "score {0} → {1} étoiles")
        @CsvSource({ "100, 5", "90, 5", "89.99, 4", "75, 4", "74.99, 3", "60, 3", "59.99, 2", "40, 2", "39.99, 1", "0, 1" })
        @DisplayName("convertit le score de performance en note sur cinq")
        void noteSelonLesPaliers(String score, int noteAttendue) {
            PerformanceFournisseurDTO resultat = mapper.toPerformanceFournisseurDTO(
                fournisseur(1, 10, 2, BigDecimal.TEN, new BigDecimal(score))
            );

            assertThat(resultat.note()).isEqualTo(noteAttendue);
        }

        @Test
        @DisplayName("sans score, la note vaut 3 — ni récompense ni sanction")
        void scoreAbsentDonneUneNoteMoyenne() {
            PerformanceFournisseurDTO resultat = mapper.toPerformanceFournisseurDTO(fournisseur(1, 10, 2, BigDecimal.TEN, null));

            assertThat(resultat.note()).isEqualTo(3);
        }

        @Test
        @DisplayName("délai et conformité absents deviennent zéro")
        void delaiEtConformiteAbsentsDeviennentZero() {
            PerformanceFournisseurDTO resultat = mapper.toPerformanceFournisseurDTO(
                fournisseur(1, 10, null, null, BigDecimal.valueOf(50))
            );

            assertThat(resultat.delaiMoyenJours()).isZero();
            assertThat(resultat.tauxConformite()).isZero();
        }

        @Test
        @DisplayName("sans fournisseur, rend null")
        void fournisseurAbsentDonneNull() {
            assertThat(mapper.toPerformanceFournisseurDTO(null)).isNull();
        }

        @Test
        @DisplayName("la conversion de liste préserve l'ordre du classement")
        void listePreserveLOrdre() {
            List<PerformanceFournisseurDTO> resultat = mapper.toPerformanceFournisseurDTOList(
                List.of(
                    fournisseur(3, 30, 1, BigDecimal.TEN, BigDecimal.valueOf(95)),
                    fournisseur(1, 20, 2, BigDecimal.TEN, BigDecimal.valueOf(70)),
                    fournisseur(2, 10, 5, BigDecimal.TEN, BigDecimal.valueOf(30))
                )
            );

            assertThat(resultat).extracting(PerformanceFournisseurDTO::fournisseurId).containsExactly(3L, 1L, 2L);
            assertThat(resultat).extracting(PerformanceFournisseurDTO::note).containsExactly(5, 3, 1);
        }

        @Test
        @DisplayName("une liste vide reste vide")
        void listeVideResteVide() {
            assertThat(mapper.toPerformanceFournisseurDTOList(List.of())).isEmpty();
        }

        private SupplierPerformanceDTO fournisseur(
            int id,
            int nbCommandes12Mois,
            Integer delaiMoyen,
            BigDecimal conformite,
            BigDecimal score
        ) {
            return new SupplierPerformanceDTO(
                id,
                "LABOREX",
                "LBX",
                "0100000000",
                "0700000000",
                2,
                10_000_000L,
                nbCommandes12Mois,
                120_000_000L,
                delaiMoyen,
                1,
                10,
                conformite,
                score
            );
        }
    }
}
