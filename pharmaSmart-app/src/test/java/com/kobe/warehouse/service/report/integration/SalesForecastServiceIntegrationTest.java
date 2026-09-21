package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.service.dto.report.ForecastSummaryDTO;
import com.kobe.warehouse.service.dto.report.SalesForecastDTO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La prévision de ventes extrapole le chiffre d'affaires des mois à venir depuis celui des mois
 * écoulés. Elle sert à décider d'un budget d'achat et d'une trésorerie — deux engagements qu'on ne
 * prend pas sur un chiffre dont on ignore la fiabilité.
 *
 * <p>C'est pourquoi l'essentiel de ce qui est éprouvé ici n'est pas la valeur prédite mais ce qui
 * l'entoure. Le <b>niveau de confiance</b> décroît quand l'historique est court ou l'horizon
 * lointain : une officine ouverte depuis trois mois ne peut pas annoncer son chiffre d'affaires de
 * l'an prochain à 95 %. La <b>qualité des données</b> le dit explicitement à l'écran. Et les
 * indicateurs statistiques — croissance, précision du modèle — sont rendus <b>nuls</b> plutôt que
 * calculés sur trop peu de points : un taux de croissance faux est plus dangereux qu'un taux absent,
 * parce qu'on le croit.
 *
 * <p>Les trois méthodes ne répondent pas à la même question. La <b>régression</b> suit une tendance,
 * la <b>moyenne mobile</b> lisse le bruit, la <b>saisonnalité</b> rejoue le même mois de l'an passé.
 * Sur un historique qui monte régulièrement, elles divergent — et c'est normal.
 *
 * <p>Le jeu d'essai reste dans les partitions du conteneur de test (année précédente à année
 * suivante), ce qui borne l'historique constructible à moins de deux ans.
 */
@DisplayName("SalesForecastService — prévision de ventes")
class SalesForecastServiceIntegrationTest extends AbstractReportIntegrationTest {

    // ===== régression linéaire =====

    @Nested
    @DisplayName("Régression linéaire")
    class RegressionLineaire {

        /**
         * Sur une série parfaitement linéaire, la droite passe par tous les points : la prévision
         * du mois suivant prolonge exactement la progression observée.
         */
        @Test
        @DisplayName("prolonge une progression régulière sans la déformer")
        void prolongeUneTendance() {
            // 6 mois : 100 000, 200 000, … 600 000 → pente 100 000 par mois
            for (int i = 6; i >= 1; i--) {
                caDuMois(moisPasse(i), (7 - i) * 100_000);
            }
            viderLeCache();

            List<SalesForecastDTO> prevision = services.salesForecastService.getForecast(3, "LINEAR_REGRESSION");

            assertThat(prevision).extracting(SalesForecastDTO::forecastedCA).containsExactly(700_000L, 800_000L, 900_000L);
        }

        @Test
        @DisplayName("les mois prévus suivent le dernier mois connu")
        void moisPrevusConsecutifs() {
            for (int i = 3; i >= 1; i--) {
                caDuMois(moisPasse(i), 100_000);
            }
            viderLeCache();

            YearMonth dernierMoisConnu = YearMonth.now().minusMonths(1);
            List<SalesForecastDTO> prevision = services.salesForecastService.getForecast(2, "LINEAR_REGRESSION");

            assertThat(prevision)
                .extracting(SalesForecastDTO::forecastPeriod)
                .containsExactly(dernierMoisConnu.plusMonths(1).toString(), dernierMoisConnu.plusMonths(2).toString());
            assertThat(prevision.getFirst().forecastDate()).isEqualTo(dernierMoisConnu.plusMonths(1).atDay(1));
        }

        /** L'intervalle encadre la prévision ; il ne descend jamais sous zéro, un CA négatif n'ayant pas de sens. */
        @Test
        @DisplayName("l'intervalle de confiance encadre la prévision et reste positif")
        void intervalleDeConfiance() {
            for (int i = 6; i >= 1; i--) {
                caDuMois(moisPasse(i), (7 - i) * 100_000);
            }
            viderLeCache();

            SalesForecastDTO premier = services.salesForecastService.getForecast(1, "LINEAR_REGRESSION").getFirst();

            assertThat(premier.lowerBound()).isLessThanOrEqualTo(premier.forecastedCA());
            assertThat(premier.upperBound()).isGreaterThanOrEqualTo(premier.forecastedCA());
            assertThat(premier.lowerBound()).isNotNegative();
        }

        /**
         * Trois paliers de confiance selon la profondeur de l'historique : moins de six mois plafonne
         * à 60 %, moins de douze à 80 %. Annoncer 95 % sur quatre mois de données serait un mensonge
         * chiffré.
         */
        @Test
        @DisplayName("la confiance est plafonnée quand l'historique est court")
        void confiancePlafonneeParLHistorique() {
            for (int i = 3; i >= 1; i--) {
                caDuMois(moisPasse(i), 100_000 * i);
            }
            viderLeCache();

            assertThat(services.salesForecastService.getForecast(1, "LINEAR_REGRESSION").getFirst().confidenceLevel())
                .isEqualByComparingTo("60");
        }

        /** Au-delà de six mois d'horizon, la confiance se dégrade de cinq points par mois supplémentaire. */
        @Test
        @DisplayName("la confiance se dégrade au-delà de six mois d'horizon")
        void confianceDegradeeParLHorizon() {
            for (int i = 8; i >= 1; i--) {
                caDuMois(moisPasse(i), 100_000);
            }
            viderLeCache();

            List<SalesForecastDTO> prevision = services.salesForecastService.getForecast(9, "LINEAR_REGRESSION");

            assertThat(prevision.get(5).confidenceLevel()).isEqualByComparingTo("80"); // 6e mois
            assertThat(prevision.get(6).confidenceLevel()).isEqualByComparingTo("75"); // 7e
            assertThat(prevision.get(8).confidenceLevel()).isEqualByComparingTo("65"); // 9e
        }

        /** Un seul point ne définit pas de pente : mieux vaut ne rien annoncer qu'annoncer n'importe quoi. */
        @Test
        @DisplayName("un seul mois d'historique ne permet aucune prévision")
        void unSeulMoisNePermetRien() {
            caDuMois(moisPasse(1), 100_000);
            viderLeCache();

            assertThat(services.salesForecastService.getForecast(3, "LINEAR_REGRESSION")).isEmpty();
        }

        @Test
        @DisplayName("la méthode est reportée sur chaque point de prévision")
        void methodeReportee() {
            for (int i = 3; i >= 1; i--) {
                caDuMois(moisPasse(i), 100_000 * i);
            }
            viderLeCache();

            assertThat(services.salesForecastService.getForecast(2, "LINEAR_REGRESSION"))
                .extracting(SalesForecastDTO::forecastMethod)
                .containsOnly("LINEAR_REGRESSION");
        }

        /** Une méthode inconnue ne doit pas faire échouer l'écran : elle retombe sur la régression. */
        @Test
        @DisplayName("une méthode inconnue retombe sur la régression")
        void methodeInconnueRetombeSurLaRegression() {
            for (int i = 3; i >= 1; i--) {
                caDuMois(moisPasse(i), 100_000 * i);
            }
            viderLeCache();

            assertThat(services.salesForecastService.getForecast(1, "METHODE_INEXISTANTE"))
                .extracting(SalesForecastDTO::forecastMethod)
                .containsExactly("LINEAR_REGRESSION");
        }
    }

    // ===== autres méthodes =====

    @Nested
    @DisplayName("Moyenne mobile et saisonnalité")
    class AutresMethodes {

        /**
         * La moyenne mobile lisse : sur une série stable, elle reconduit le niveau observé au lieu
         * d'extrapoler une pente que le bruit aurait fait apparaître.
         */
        @Test
        @DisplayName("la moyenne mobile reconduit le niveau observé")
        void moyenneMobileReconduitLeNiveau() {
            for (int i = 6; i >= 1; i--) {
                caDuMois(moisPasse(i), 100_000);
            }
            viderLeCache();

            List<SalesForecastDTO> prevision = services.salesForecastService.getForecast(3, "MOVING_AVERAGE");

            assertThat(prevision).extracting(SalesForecastDTO::forecastedCA).containsExactly(100_000L, 100_000L, 100_000L);
            assertThat(prevision).extracting(SalesForecastDTO::forecastMethod).containsOnly("MOVING_AVERAGE");
        }

        @Test
        @DisplayName("la moyenne mobile borne sa prévision à dix pour cent près")
        void moyenneMobileMarge() {
            for (int i = 6; i >= 1; i--) {
                caDuMois(moisPasse(i), 100_000);
            }
            viderLeCache();

            SalesForecastDTO premier = services.salesForecastService.getForecast(1, "MOVING_AVERAGE").getFirst();

            assertThat(premier.lowerBound()).isEqualTo(90_000L);
            assertThat(premier.upperBound()).isEqualTo(110_000L);
        }

        /**
         * La méthode saisonnière rejoue le même mois calendaire observé un an plus tôt. Sur un
         * historique de douze mois, la prévision du mois suivant reprend donc son homologue.
         */
        @Test
        @DisplayName("la méthode saisonnière rejoue le même mois de l'année précédente")
        void saisonnaliteRejoueLeMemeMois() {
            for (int i = 12; i >= 1; i--) {
                caDuMois(moisPasse(i), i == 12 ? 900_000 : 100_000);
            }
            viderLeCache();

            // Le mois prévu est celui qui suit le dernier connu — soit le même mois calendaire
            // que le plus ancien de l'historique, douze mois plus tôt.
            SalesForecastDTO premier = services.salesForecastService.getForecast(1, "SEASONAL").getFirst();

            assertThat(premier.forecastedCA()).isEqualTo(900_000L);
            assertThat(premier.forecastMethod()).isEqualTo("SEASONAL");
        }

        @Test
        @DisplayName("un mois calendaire jamais observé se prévoit à zéro")
        void moisJamaisObserve() {
            caDuMois(moisPasse(2), 100_000);
            caDuMois(moisPasse(1), 100_000);
            viderLeCache();

            // Trois mois d'horizon : le troisième n'a pas d'homologue dans un historique de deux mois.
            List<SalesForecastDTO> prevision = services.salesForecastService.getForecast(3, "SEASONAL");

            assertThat(prevision.get(2).forecastedCA()).isZero();
        }

        /**
         * Une officine qui vient d'ouvrir, ou une base fraîchement installée, n'a aucun historique.
         * L'écran de prévision doit s'y ouvrir sur un message vide, pas sur une erreur.
         */
        @Test
        @DisplayName("sans aucun historique, les trois méthodes rendent une prévision vide")
        void sansHistoriqueAucuneMethodeNEchoue() {
            assertThatCode(() -> assertThat(services.salesForecastService.getForecast(3, "LINEAR_REGRESSION")).isEmpty())
                .doesNotThrowAnyException();
            assertThatCode(() -> assertThat(services.salesForecastService.getForecast(3, "MOVING_AVERAGE")).isEmpty())
                .doesNotThrowAnyException();
            assertThatCode(() -> assertThat(services.salesForecastService.getForecast(3, "SEASONAL")).isEmpty())
                .doesNotThrowAnyException();
        }
    }

    // ===== synthèse =====

    @Nested
    @DisplayName("Synthèse")
    class Synthese {

        @Test
        @DisplayName("la synthèse cumule les prévisions à trois, six et douze mois")
        void cumulsParHorizon() {
            for (int i = 6; i >= 1; i--) {
                caDuMois(moisPasse(i), 100_000);
            }
            viderLeCache();

            ForecastSummaryDTO synthese = services.salesForecastService.getForecastSummary();

            assertThat(synthese.totalForecastedCA3M()).isEqualTo(300_000L);
            assertThat(synthese.totalForecastedCA6M()).isEqualTo(600_000L);
            assertThat(synthese.totalForecastedCA12M()).isEqualTo(1_200_000L);
            assertThat(synthese.dataPointsUsed()).isEqualTo(6);
        }

        /**
         * Les quatre paliers de qualité sont ce qui permet au lecteur de savoir s'il peut engager un
         * budget sur le chiffre affiché. Ils sont testés sur leurs bornes.
         */
        @Test
        @DisplayName("la qualité des données qualifie la profondeur de l'historique")
        void qualiteDesDonnees() {
            caDuMois(moisPasse(1), 100_000);
            caDuMois(moisPasse(2), 100_000);
            viderLeCache();
            assertThat(services.salesForecastService.getForecastSummary().dataQuality()).isEqualTo("INSUFFICIENT");

            caDuMois(moisPasse(3), 100_000);
            caDuMois(moisPasse(4), 100_000);
            viderLeCache();
            assertThat(services.salesForecastService.getForecastSummary().dataQuality()).isEqualTo("LOW");

            for (int i = 5; i <= 12; i++) {
                caDuMois(moisPasse(i), 100_000);
            }
            viderLeCache();
            assertThat(services.salesForecastService.getForecastSummary().dataQuality()).isEqualTo("MEDIUM");
        }

        /**
         * Un taux de croissance calculé sur trois points serait mathématiquement exact et
         * statistiquement trompeur. Le rendre nul est un refus de répondre, et c'est le bon.
         */
        @Test
        @DisplayName("croissance et précision sont tues quand l'historique est trop court")
        void indicateursTusSiHistoriqueTropCourt() {
            caDuMois(moisPasse(1), 100_000);
            caDuMois(moisPasse(2), 200_000);
            viderLeCache();

            ForecastSummaryDTO synthese = services.salesForecastService.getForecastSummary();

            assertThat(synthese.averageMonthlyGrowthPct()).isNull();
            assertThat(synthese.predictedYearlyGrowthPct()).isNull();
            assertThat(synthese.modelAccuracyPct()).isNull();
            assertThat(synthese.meanAbsoluteError()).isNull();
        }

        @Test
        @DisplayName("au-delà de quatre mois, croissance et précision sont chiffrées")
        void indicateursChiffresDesQuatreMois() {
            for (int i = 6; i >= 1; i--) {
                caDuMois(moisPasse(i), (7 - i) * 100_000);
            }
            viderLeCache();

            ForecastSummaryDTO synthese = services.salesForecastService.getForecastSummary();

            assertThat(synthese.averageMonthlyGrowthPct()).isNotNull();
            assertThat(synthese.predictedYearlyGrowthPct()).isNotNull();
            assertThat(synthese.modelAccuracyPct()).isNotNull();
            assertThat(synthese.meanAbsoluteError()).isNotNull();
        }

        @Test
        @DisplayName("les mois haut et bas nomment les extrêmes de l'historique")
        void moisHautEtBas() {
            for (int i = 6; i >= 1; i--) {
                caDuMois(moisPasse(i), (7 - i) * 100_000);
            }
            viderLeCache();

            ForecastSummaryDTO synthese = services.salesForecastService.getForecastSummary();

            assertThat(synthese.peakMonth()).isEqualTo(nomDuMois(moisPasse(1)));
            assertThat(synthese.lowMonth()).isEqualTo(nomDuMois(moisPasse(6)));
        }

        @Test
        @DisplayName("base vierge : une synthèse sans prévision et sans indicateur")
        void baseVierge() {
            ForecastSummaryDTO synthese = services.salesForecastService.getForecastSummary();

            assertThat(synthese.dataPointsUsed()).isZero();
            assertThat(synthese.dataQuality()).isEqualTo("INSUFFICIENT");
            assertThat(synthese.totalForecastedCA3M()).isZero();
            assertThat(synthese.averageMonthlyGrowthPct()).isNull();
            assertThat(synthese.peakMonth()).isNull();
            assertThat(synthese.seasonalityDetected()).isFalse();
        }
    }

    // ===== saisonnalité et historique =====

    @Nested
    @DisplayName("Saisonnalité et historique")
    class SaisonnaliteEtHistorique {

        /** Moins de douze mois : aucun cycle annuel n'est observable, la réponse est non par construction. */
        @Test
        @DisplayName("la saisonnalité n'est pas cherchée sur moins de douze mois")
        void saisonnaliteNonChercheeSousDouzeMois() {
            for (int i = 6; i >= 1; i--) {
                caDuMois(moisPasse(i), i % 2 == 0 ? 100_000 : 900_000);
            }
            viderLeCache();

            assertThat(services.salesForecastService.detectSeasonality()).isFalse();
        }

        @Test
        @DisplayName("base vierge : aucune saisonnalité, sans erreur")
        void saisonnaliteBaseVierge() {
            assertThat(services.salesForecastService.detectSeasonality()).isFalse();
        }

        @Test
        @DisplayName("l'historique demandé est rendu avant les prévisions, sur la période choisie")
        void historiquePuisPrevisions() {
            for (int i = 4; i >= 1; i--) {
                caDuMois(moisPasse(i), 100_000 * i);
            }
            viderLeCache();

            List<SalesForecastDTO> combine = services.salesForecastService.getHistoricalVsForecast(
                moisPasse(3).atDay(1),
                moisPasse(1).atEndOfMonth(),
                2
            );

            assertThat(combine).hasSize(5); // 3 mois d'historique + 2 de prévision
            assertThat(combine.subList(0, 3)).extracting(SalesForecastDTO::forecastMethod).containsOnly("HISTORICAL");
            assertThat(combine.subList(3, 5)).extracting(SalesForecastDTO::forecastMethod).containsOnly("LINEAR_REGRESSION");
        }

        /** Un point historique n'est pas une prédiction : sa valeur réelle et sa valeur « prévue » coïncident. */
        @Test
        @DisplayName("un point historique porte sa valeur réelle et une confiance totale")
        void pointHistorique() {
            caDuMois(moisPasse(2), 250_000);
            caDuMois(moisPasse(1), 300_000);
            viderLeCache();

            SalesForecastDTO point = services.salesForecastService
                .getHistoricalVsForecast(moisPasse(2).atDay(1), moisPasse(2).atEndOfMonth(), 1)
                .getFirst();

            assertThat(point.forecastedCA()).isEqualTo(250_000L);
            assertThat(point.actualCA()).isEqualTo(250_000L);
            assertThat(point.confidenceLevel()).isEqualByComparingTo("100");
        }

        @Test
        @DisplayName("une période sans vente ne rend que les prévisions")
        void periodeSansVente() {
            caDuMois(moisPasse(2), 250_000);
            caDuMois(moisPasse(1), 300_000);
            viderLeCache();

            List<SalesForecastDTO> combine = services.salesForecastService.getHistoricalVsForecast(
                moisPasse(10).atDay(1),
                moisPasse(9).atEndOfMonth(),
                1
            );

            assertThat(combine).hasSize(1);
            assertThat(combine.getFirst().forecastMethod()).isEqualTo("LINEAR_REGRESSION");
        }
    }

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre")
    class Perimetre {

        @Test
        @DisplayName("une vente annulée ne pèse pas dans l'historique")
        void venteAnnuleeExclue() {
            caDuMois(moisPasse(2), 100_000);
            caDuMois(moisPasse(1), 100_000);
            venteMontant(moisPasse(1).atDay(15), 900_000, true, CategorieChiffreAffaire.CA);
            viderLeCache();

            assertThat(services.salesForecastService.getForecast(1, "MOVING_AVERAGE").getFirst().forecastedCA())
                .isEqualTo(100_000L);
        }

        @Test
        @DisplayName("une vente hors chiffre d'affaires ne pèse pas dans l'historique")
        void venteHorsChiffreDAffairesExclue() {
            caDuMois(moisPasse(2), 100_000);
            caDuMois(moisPasse(1), 100_000);
            venteMontant(moisPasse(1).atDay(15), 900_000, false, CategorieChiffreAffaire.CA_DEPOT);
            viderLeCache();

            assertThat(services.salesForecastService.getForecast(1, "MOVING_AVERAGE").getFirst().forecastedCA())
                .isEqualTo(100_000L);
        }
    }

    // ===== fabriques locales =====

    private static YearMonth moisPasse(int nombreDeMois) {
        return YearMonth.now().minusMonths(nombreDeMois);
    }

    private static String nomDuMois(YearMonth mois) {
        return mois.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH);
    }

    /** Une vente unique portant tout le chiffre d'affaires du mois — l'historique ne compte que des totaux. */
    private void caDuMois(YearMonth mois, int montant) {
        venteMontant(mois.atDay(15), montant, false, CategorieChiffreAffaire.CA);
    }

    private void venteMontant(LocalDate date, int montant, boolean annulee, CategorieChiffreAffaire categorie) {
        var vente = venteFermee(date, annulee, categorie);
        vente.setSalesAmount(montant);
        vente.setNetAmount(montant);
        vente.setAmountToBePaid(montant);
        vente.setPayrollAmount(montant);
        em.flush();
    }
}
