package com.kobe.warehouse.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.repository.DashboardCARepository;
import com.kobe.warehouse.service.dto.FinancesSummaryDTO;
import com.kobe.warehouse.service.dto.dashboard.PerformanceVendeurDTO;
import com.kobe.warehouse.service.dto.report.BasketEvolutionDTO;
import com.kobe.warehouse.service.dto.report.DailyCADTO;
import com.kobe.warehouse.service.dto.report.DashboardCAEvolutionDTO;
import com.kobe.warehouse.service.dto.report.DashboardCASummaryDTO;
import com.kobe.warehouse.service.dto.report.GenericsSubstitutionDTO;
import com.kobe.warehouse.service.dto.report.PaymentMethodCADTO;
import com.kobe.warehouse.service.dto.report.ProductFamilyCADTO;
import com.kobe.warehouse.service.dto.report.RemisesAnalysisKpiDTO;
import com.kobe.warehouse.service.dto.report.TopProductDTO;
import com.kobe.warehouse.service.dto.report.TopRemiseProduitDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Le tableau de bord du chiffre d'affaires est l'écran qu'on ouvre en premier le matin : le chiffre
 * du jour, de la semaine, du mois, de l'année, et l'écart avec la période équivalente précédente.
 *
 * <p>Ces écarts n'ont de sens que si les périodes comparées sont les bonnes : la semaine commence un
 * lundi, « le mois dernier » s'arrête la veille du premier du mois courant, et une semaine à cheval
 * sur deux années appartient à l'une ou à l'autre mais jamais aux deux. Une borne décalée ne lève
 * aucune erreur — elle affiche une flèche verte là où il en fallait une rouge.
 */
@DisplayName("DashboardCAService — tableau de bord du chiffre d'affaires")
class DashboardCAServiceImplTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 3, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 3, 31);

    // Doublure créée à l'initialisation du champ, et non dans un @BeforeEach : le délai de quinze
    // secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final DashboardCARepository repository = mock(DashboardCARepository.class);

    private final DashboardCAService service = new DashboardCAServiceImpl(repository);

    @BeforeEach
    void officineAuRepos() {
        when(repository.findDailySummary(any(), any())).thenReturn(List.of());
        when(repository.getPeriodAggregation(any(), any())).thenReturn(periode(0L, 0, "0", "0"));
        when(repository.findPaymentMethodDistribution(any(), any())).thenReturn(List.of());
        when(repository.findProductFamilyDistribution(any(), any())).thenReturn(List.of());
        when(repository.findTopProducts(any(), any(), anyInt())).thenReturn(List.of());
        when(repository.findBasketEvolutionMonthly()).thenReturn(List.of());
        when(repository.findSalesByStaff(any(), any())).thenReturn(List.of());
        when(repository.findRemisesTopProducts(any(), any(), anyInt())).thenReturn(List.of());
    }

    // ===== synthèse journalière =====

    @Nested
    @DisplayName("Synthèse journalière")
    class SyntheseJournaliere {

        @Test
        @DisplayName("chaque colonne du tableau brut atterrit dans le champ qui la désigne")
        void lectureDesColonnes() {
            when(repository.findDailySummary(DEBUT, FIN)).thenReturn(
                List.<Object[]>of(
                    new Object[] {
                        java.sql.Date.valueOf(LocalDate.of(2026, 3, 15)),
                        42,
                        1_000_000L,
                        950_000L,
                        new BigDecimal("22619.05"),
                        600_000L,
                        350_000L,
                        new BigDecimal("36.84"),
                        38,
                        900_000L,
                        50_000L
                    }
                )
            );

            DailyCADTO jour = service.getDailySummary(DEBUT, FIN).getFirst();

            assertThat(jour.saleDate()).isEqualTo(LocalDate.of(2026, 3, 15));
            assertThat(jour.nbTransactions()).isEqualTo(42);
            assertThat(jour.caTotal()).isEqualTo(1_000_000L);
            assertThat(jour.caNet()).isEqualTo(950_000L);
            assertThat(jour.panierMoyen()).isEqualByComparingTo("22619.05");
            assertThat(jour.coutTotal()).isEqualTo(600_000L);
            assertThat(jour.margeBrute()).isEqualTo(350_000L);
            assertThat(jour.tauxMargePct()).isEqualByComparingTo("36.84");
            assertThat(jour.nbClients()).isEqualTo(38);
            assertThat(jour.montantEncaisse()).isEqualTo(900_000L);
            assertThat(jour.montantCredit()).isEqualTo(50_000L);
        }

        @Test
        @DisplayName("une période sans vente rend une liste vide")
        void periodeSansVente() {
            assertThat(service.getDailySummary(DEBUT, FIN)).isEmpty();
        }
    }

    // ===== synthèse générale =====

    @Nested
    @DisplayName("Synthèse générale")
    class SyntheseGenerale {

        @Test
        @DisplayName("la semaine court depuis le lundi, le mois depuis le premier, l'année depuis le 1er janvier")
        void bornesDesPeriodes() {
            LocalDate aujourdHui = LocalDate.now();

            service.getOverallSummary();

            assertThat(bornesInterrogees()).contains(
                List.of(aujourdHui, aujourdHui),
                List.of(aujourdHui.minusDays(aujourdHui.getDayOfWeek().getValue() - 1), aujourdHui),
                List.of(aujourdHui.withDayOfMonth(1), aujourdHui),
                List.of(aujourdHui.withDayOfYear(1), aujourdHui)
            );
        }

        /** Le mois précédent s'arrête la veille du premier du mois courant, jamais le même quantième. */
        @Test
        @DisplayName("chaque période de référence s'arrête la veille de la période courante")
        void bornesDesPeriodesPrecedentes() {
            LocalDate aujourdHui = LocalDate.now();
            LocalDate lundi = aujourdHui.minusDays(aujourdHui.getDayOfWeek().getValue() - 1);
            LocalDate premierDuMois = aujourdHui.withDayOfMonth(1);
            LocalDate premierDeLAnnee = aujourdHui.withDayOfYear(1);

            service.getOverallSummary();

            assertThat(bornesInterrogees()).contains(
                List.of(aujourdHui.minusDays(1), aujourdHui.minusDays(1)),
                List.of(lundi.minusDays(7), lundi.minusDays(1)),
                List.of(premierDuMois.minusMonths(1), premierDuMois.minusDays(1)),
                List.of(premierDeLAnnee.minusYears(1), premierDeLAnnee.minusDays(1))
            );
        }

        /**
         * Un lundi, la semaine débute aujourd'hui ; un 1er janvier, le mois et l'année aussi. Les
         * bornes sont alors demandées deux fois et un {@code verify} strict échouerait ces jours-là :
         * on relève donc l'ensemble des couples interrogés sans compter les répétitions.
         */
        private List<List<LocalDate>> bornesInterrogees() {
            ArgumentCaptor<LocalDate> debuts = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> fins = ArgumentCaptor.forClass(LocalDate.class);
            verify(repository, atLeastOnce()).getPeriodAggregation(debuts.capture(), fins.capture());
            return IntStream.range(0, debuts.getAllValues().size())
                .mapToObj(i -> List.of(debuts.getAllValues().get(i), fins.getAllValues().get(i)))
                .toList();
        }

        @Test
        @DisplayName("l'écart avec la période précédente se lit en pourcentage, arrondi au centième")
        void ecartEnPourcentage() {
            LocalDate aujourdHui = LocalDate.now();
            when(repository.getPeriodAggregation(aujourdHui, aujourdHui)).thenReturn(periode(1_200_000L, 40, "30000", "35"));
            when(repository.getPeriodAggregation(aujourdHui.minusDays(1), aujourdHui.minusDays(1)))
                .thenReturn(periode(900_000L, 30, "30000", "34"));

            DashboardCASummaryDTO resume = service.getOverallSummary();

            assertThat(resume.caToday()).isEqualTo(1_200_000L);
            assertThat(resume.caTodayPrevious()).isEqualTo(900_000L);
            assertThat(resume.caTodayEvolutionPct()).isEqualByComparingTo("33.33");
        }

        @Test
        @DisplayName("un recul se lit en pourcentage négatif")
        void recul() {
            LocalDate aujourdHui = LocalDate.now();
            when(repository.getPeriodAggregation(aujourdHui, aujourdHui)).thenReturn(periode(750_000L, 20, "0", "0"));
            when(repository.getPeriodAggregation(aujourdHui.minusDays(1), aujourdHui.minusDays(1)))
                .thenReturn(periode(1_000_000L, 30, "0", "0"));

            assertThat(service.getOverallSummary().caTodayEvolutionPct()).isEqualByComparingTo("-25.00");
        }

        /** Un jour de fermeture la veille ne doit pas faire diviser par zéro. */
        @Test
        @DisplayName("une période de référence vide rend un écart nul plutôt qu'un infini")
        void referenceVide() {
            LocalDate aujourdHui = LocalDate.now();
            when(repository.getPeriodAggregation(aujourdHui, aujourdHui)).thenReturn(periode(500_000L, 10, "0", "0"));

            assertThat(service.getOverallSummary().caTodayEvolutionPct()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("les compteurs et taux de chaque période sont repris")
        void compteursEtTaux() {
            LocalDate aujourdHui = LocalDate.now();
            when(repository.getPeriodAggregation(aujourdHui, aujourdHui)).thenReturn(periode(1_000_000L, 42, "23809.52", "36.84"));

            DashboardCASummaryDTO resume = service.getOverallSummary();

            assertThat(resume.nbTransactionsToday()).isEqualTo(42);
            assertThat(resume.panierMoyenToday()).isEqualByComparingTo("23809.52");
            assertThat(resume.tauxMargeToday()).isEqualByComparingTo("36.84");
        }

        @Test
        @DisplayName("une colonne non renseignée vaut zéro")
        void colonneAbsente() {
            when(repository.getPeriodAggregation(any(), any())).thenReturn(new Object[] { null, null, null, null });

            DashboardCASummaryDTO resume = service.getOverallSummary();

            assertThat(resume.caToday()).isZero();
            assertThat(resume.nbTransactionsToday()).isZero();
            assertThat(resume.panierMoyenToday()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ===== courbe d'évolution =====

    @Nested
    @DisplayName("Courbe d'évolution")
    class CourbeDEvolution {

        @Test
        @DisplayName("au jour le jour, chaque journée donne un point")
        void auJourLeJour() {
            donneJournees(jour(LocalDate.of(2026, 3, 1), 3, 100_000L), jour(LocalDate.of(2026, 3, 2), 4, 150_000L));

            DashboardCAEvolutionDTO evolution = service.getEvolutionData("daily", DEBUT, FIN);

            assertThat(evolution.period()).isEqualTo("daily");
            assertThat(evolution.labels()).containsExactly("2026-03-01", "2026-03-02");
            assertThat(evolution.caValues()).containsExactly(100_000L, 150_000L);
            assertThat(evolution.transactionCounts()).containsExactly(3, 4);
        }

        @Test
        @DisplayName("au mois, les journées d'un même mois se cumulent")
        void auMois() {
            donneJournees(
                jour(LocalDate.of(2026, 3, 1), 3, 100_000L),
                jour(LocalDate.of(2026, 3, 15), 4, 150_000L),
                jour(LocalDate.of(2026, 4, 2), 5, 200_000L)
            );

            DashboardCAEvolutionDTO evolution = service.getEvolutionData("monthly", DEBUT, FIN);

            assertThat(evolution.labels()).containsExactly("2026-03", "2026-04");
            assertThat(evolution.caValues()).containsExactly(250_000L, 200_000L);
            assertThat(evolution.transactionCounts()).containsExactly(7, 5);
        }

        @Test
        @DisplayName("à la semaine, les journées d'une même semaine se cumulent")
        void aLaSemaine() {
            // Lundi 9 et mercredi 11 mars 2026 : même semaine ISO.
            donneJournees(
                jour(LocalDate.of(2026, 3, 9), 3, 100_000L),
                jour(LocalDate.of(2026, 3, 11), 4, 150_000L),
                jour(LocalDate.of(2026, 3, 16), 5, 200_000L)
            );

            DashboardCAEvolutionDTO evolution = service.getEvolutionData("weekly", DEBUT, FIN);

            assertThat(evolution.period()).isEqualTo("weekly");
            assertThat(evolution.labels()).containsExactly("2026-W11", "2026-W12");
            assertThat(evolution.caValues()).containsExactly(250_000L, 200_000L);
        }

        /**
         * Le défaut que ce test fixe : la semaine était numérotée sur l'année civile. Le 1er janvier
         * 2027 tombant un vendredi, il appartient à la semaine 53 de 2026 — l'ancienne formule en
         * faisait une semaine « 2027-W00 », qui n'existe pas, et séparait ces trois jours des quatre
         * autres de leur propre semaine.
         */
        @Test
        @DisplayName("une semaine à cheval sur deux années reste une seule semaine")
        void semaineACheval() {
            donneJournees(
                jour(LocalDate.of(2026, 12, 31), 2, 50_000L),
                jour(LocalDate.of(2027, 1, 1), 3, 80_000L),
                jour(LocalDate.of(2027, 1, 4), 4, 90_000L)
            );

            DashboardCAEvolutionDTO evolution = service.getEvolutionData("weekly", DEBUT, FIN);

            assertThat(evolution.labels()).containsExactly("2026-W53", "2027-W01");
            assertThat(evolution.caValues()).containsExactly(130_000L, 90_000L);
        }

        @Test
        @DisplayName("les libellés sont rendus dans l'ordre chronologique")
        void ordreChronologique() {
            donneJournees(
                jour(LocalDate.of(2026, 4, 2), 1, 1L),
                jour(LocalDate.of(2026, 1, 15), 1, 1L),
                jour(LocalDate.of(2026, 2, 20), 1, 1L)
            );

            assertThat(service.getEvolutionData("monthly", DEBUT, FIN).labels())
                .containsExactly("2026-01", "2026-02", "2026-04");
        }

        @Test
        @DisplayName("une période inconnue retombe sur le jour le jour")
        void periodeInconnue() {
            donneJournees(jour(LocalDate.of(2026, 3, 1), 1, 1L));

            assertThat(service.getEvolutionData("trimestre", DEBUT, FIN).period()).isEqualTo("daily");
        }

        private void donneJournees(Object[]... journees) {
            when(repository.findDailySummary(DEBUT, FIN)).thenReturn(List.of(journees));
        }
    }

    // ===== répartitions =====

    @Nested
    @DisplayName("Répartitions")
    class Repartitions {

        @Test
        @DisplayName("la répartition par mode de règlement est reprise ligne à ligne")
        void parModeDeReglement() {
            when(repository.findPaymentMethodDistribution(DEBUT, FIN)).thenReturn(
                List.<Object[]>of(
                    new Object[] {
                        java.sql.Date.valueOf(LocalDate.of(2026, 3, 15)),
                        "Espèces",
                        "CASH",
                        30,
                        750_000L,
                        new BigDecimal("25000.00")
                    }
                )
            );

            PaymentMethodCADTO mode = service.getPaymentMethodDistribution(DEBUT, FIN).getFirst();

            assertThat(mode.paymentDate()).isEqualTo(LocalDate.of(2026, 3, 15));
            assertThat(mode.paymentMethod()).isEqualTo("Espèces");
            assertThat(mode.paymentCode()).isEqualTo("CASH");
            assertThat(mode.nbPayments()).isEqualTo(30);
            assertThat(mode.montantTotal()).isEqualTo(750_000L);
            assertThat(mode.montantMoyen()).isEqualByComparingTo("25000.00");
        }

        @Test
        @DisplayName("la répartition par famille est reprise ligne à ligne")
        void parFamille() {
            when(repository.findProductFamilyDistribution(DEBUT, FIN)).thenReturn(
                List.<Object[]>of(
                    new Object[] {
                        java.sql.Date.valueOf(LocalDate.of(2026, 3, 15)),
                        "ANTALGIQUES",
                        120,
                        800_000L,
                        500_000L,
                        300_000L,
                        new BigDecimal("37.50"),
                        45
                    }
                )
            );

            ProductFamilyCADTO famille = service.getProductFamilyDistribution(DEBUT, FIN).getFirst();

            assertThat(famille.famille()).isEqualTo("ANTALGIQUES");
            assertThat(famille.quantiteVendue()).isEqualTo(120);
            assertThat(famille.caTotal()).isEqualTo(800_000L);
            assertThat(famille.coutTotal()).isEqualTo(500_000L);
            assertThat(famille.margeBrute()).isEqualTo(300_000L);
            assertThat(famille.tauxMargePct()).isEqualByComparingTo("37.50");
            assertThat(famille.nbLignesVente()).isEqualTo(45);
        }

        @Test
        @DisplayName("le palmarès des produits est repris ligne à ligne")
        void palmaresDesProduits() {
            when(repository.findTopProducts(DEBUT, FIN, 10)).thenReturn(
                List.<Object[]>of(
                    new Object[] {
                        java.sql.Date.valueOf(LocalDate.of(2026, 3, 1)),
                        42,
                        "DOLIPRANE",
                        "CIP1",
                        120L,
                        340,
                        3_400_000,
                        new BigDecimal("10000.00")
                    }
                )
            );

            TopProductDTO produit = service.getTopProducts(DEBUT, FIN, null).getFirst();

            assertThat(produit.produitId()).isEqualTo(42);
            assertThat(produit.libelle()).isEqualTo("DOLIPRANE");
            assertThat(produit.codeCip()).isEqualTo("CIP1");
            assertThat(produit.nbVentes()).isEqualTo(120L);
            assertThat(produit.qteVendue()).isEqualTo(340);
            assertThat(produit.caGenere()).isEqualTo(3_400_000);
        }

        /** Sans limite précisée, le palmarès s'arrête à dix produits. */
        @Test
        @DisplayName("sans limite précisée, le palmarès en demande dix")
        void limiteParDefaut() {
            service.getTopProducts(DEBUT, FIN, null);

            verify(repository).findTopProducts(DEBUT, FIN, 10);
        }

        @Test
        @DisplayName("la limite demandée est transmise")
        void limiteDemandee() {
            service.getTopProducts(DEBUT, FIN, 50);

            verify(repository).findTopProducts(DEBUT, FIN, 50);
        }
    }

    // ===== panier moyen =====

    @Nested
    @DisplayName("Évolution du panier moyen")
    class EvolutionDuPanier {

        @Test
        @DisplayName("le dernier mois est le courant, l'avant-dernier la référence")
        void dernierEtAvantDernier() {
            donnePaniers("20000", "22000", "24000");

            BasketEvolutionDTO evolution = service.getBasketEvolution();

            assertThat(evolution.currentValue()).isEqualByComparingTo("24000");
            assertThat(evolution.previousValue()).isEqualByComparingTo("22000");
            assertThat(evolution.evolutionAmount()).isEqualByComparingTo("2000");
            assertThat(evolution.evolutionPct()).isEqualByComparingTo("9.09");
        }

        @Test
        @DisplayName("le meilleur mois est retenu avec son libellé")
        void meilleurMois() {
            donnePaniers("20000", "30000", "24000");

            BasketEvolutionDTO evolution = service.getBasketEvolution();

            assertThat(evolution.bestMonthValue()).isEqualByComparingTo("30000");
            assertThat(evolution.bestMonthLabel()).isEqualTo(evolution.labels().get(1));
        }

        /** La tendance confronte la moyenne du dernier trimestre à celle du précédent. */
        @Test
        @DisplayName("la tendance se lit sur deux trimestres, à partir de six mois de recul")
        void tendanceSurSixMois() {
            donnePaniers("10000", "10000", "10000", "12000", "12000", "12000");

            assertThat(service.getBasketEvolution().trend6MPct()).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("moins de six mois de recul ne permet aucune tendance")
        void reculInsuffisant() {
            donnePaniers("10000", "12000", "14000", "16000", "18000");

            assertThat(service.getBasketEvolution().trend6MPct()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("un seul mois n'a pas de référence à laquelle se comparer")
        void unSeulMois() {
            donnePaniers("20000");

            BasketEvolutionDTO evolution = service.getBasketEvolution();

            assertThat(evolution.currentValue()).isEqualByComparingTo("20000");
            assertThat(evolution.previousValue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(evolution.evolutionPct()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("aucune donnée rend une courbe vide sans échouer")
        void aucuneDonnee() {
            BasketEvolutionDTO evolution = service.getBasketEvolution();

            assertThat(evolution.labels()).isEmpty();
            assertThat(evolution.currentValue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(evolution.bestMonthLabel()).isEmpty();
        }

        private void donnePaniers(String... paniers) {
            List<Object[]> rows = new java.util.ArrayList<>();
            for (int i = 0; i < paniers.length; i++) {
                rows.add(new Object[] { 2026, i + 1, paniers[i] });
            }
            when(repository.findBasketEvolutionMonthly()).thenReturn(rows);
        }
    }

    // ===== indicateurs annexes =====

    @Nested
    @DisplayName("Indicateurs annexes")
    class IndicateursAnnexes {

        @Test
        @DisplayName("la situation financière rassemble dettes, créances et impayés")
        void situationFinanciere() {
            when(repository.getDetteFournisseur()).thenReturn(8_000_000L);
            when(repository.getCreanceTiersPayant()).thenReturn(12_000_000L);
            when(repository.getNbEcheancesEnRetard()).thenReturn(4L);
            when(repository.getNbFacturesImpayees()).thenReturn(17L);

            FinancesSummaryDTO finances = service.getSummaryFinances();

            assertThat(finances.totalDetteFournisseurs()).isEqualTo(8_000_000L);
            assertThat(finances.totalCreancesTP()).isEqualTo(12_000_000L);
            assertThat(finances.nbEcheancesEnRetard()).isEqualTo(4L);
            assertThat(finances.nbFacturesImpayees()).isEqualTo(17L);
        }

        @Test
        @DisplayName("la performance par vendeur est reprise ligne à ligne")
        void performanceParVendeur() {
            when(repository.findSalesByStaff(DEBUT, FIN)).thenReturn(
                List.<Object[]>of(new Object[] { 7L, "Awa Koné", 120, 3_000_000L, 25_000L, 2.5 })
            );

            PerformanceVendeurDTO vendeur = service.getSalesByStaff(DEBUT, FIN).getFirst();

            assertThat(vendeur.vendeurId()).isEqualTo(7L);
            assertThat(vendeur.vendeurNom()).isEqualTo("Awa Koné");
            assertThat(vendeur.nombreVentes()).isEqualTo(120);
            assertThat(vendeur.montantTotal()).isEqualTo(3_000_000L);
            assertThat(vendeur.ticketMoyen()).isEqualTo(25_000L);
            assertThat(vendeur.tauxRemise()).isEqualTo(2.5);
        }

        @Test
        @DisplayName("un taux de remise non renseigné vaut zéro")
        void tauxDeRemiseAbsent() {
            when(repository.findSalesByStaff(DEBUT, FIN)).thenReturn(
                List.<Object[]>of(new Object[] { 7L, "Awa Koné", 1, 1L, 1L, null })
            );

            assertThat(service.getSalesByStaff(DEBUT, FIN).getFirst().tauxRemise()).isZero();
        }

        @Test
        @DisplayName("la substitution générique rend ses compteurs et ses taux dérivés")
        void substitutionGenerique() {
            when(repository.findGenericsSubstitutionStats(DEBUT, FIN)).thenReturn(
                new Object[] { 1_000L, 400L, 250L, 10_000_000L, 3_000_000L, 2_000_000L }
            );

            GenericsSubstitutionDTO generiques = service.getGenericsSubstitution(DEBUT, FIN);

            assertThat(generiques.totalProduits()).isEqualTo(1_000L);
            assertThat(generiques.produitsGeneriques()).isEqualTo(400L);
            assertThat(generiques.tauxGeneriques()).isEqualTo(40.0);
            assertThat(generiques.tauxPrincepsSubstituables()).isEqualTo(25.0);
            assertThat(generiques.tauxCaGeneriques()).isEqualTo(30.0);
        }

        @Test
        @DisplayName("une officine sans produit ne divise pas par zéro")
        void aucunProduit() {
            when(repository.findGenericsSubstitutionStats(DEBUT, FIN)).thenReturn(
                new Object[] { 0L, 0L, 0L, 0L, 0L, 0L }
            );

            GenericsSubstitutionDTO generiques = service.getGenericsSubstitution(DEBUT, FIN);

            assertThat(generiques.tauxGeneriques()).isZero();
            assertThat(generiques.tauxCaGeneriques()).isZero();
        }

        @Test
        @DisplayName("les remises rendent leur montant, leur taux et leur portée")
        void remises() {
            when(repository.findRemisesKpi(DEBUT, FIN)).thenReturn(
                new Object[] { 500_000L, 9_500_000L, 5.0, 120, 800 }
            );

            RemisesAnalysisKpiDTO remises = service.getRemisesKpi(DEBUT, FIN);

            assertThat(remises.totalRemise()).isEqualTo(500_000L);
            assertThat(remises.caApresRemise()).isEqualTo(9_500_000L);
            assertThat(remises.tauxRemise()).isEqualTo(5.0);
            assertThat(remises.nbVentesAvecRemise()).isEqualTo(120);
            assertThat(remises.nbVentesTotal()).isEqualTo(800);
        }

        @Test
        @DisplayName("les produits les plus remisés sont repris avec leur montant")
        void produitsRemises() {
            when(repository.findRemisesTopProducts(DEBUT, FIN, 5)).thenReturn(
                List.<Object[]>of(new Object[] { "DOLIPRANE", 120_000L, 45 })
            );

            TopRemiseProduitDTO produit = service.getRemisesTopProducts(DEBUT, FIN, 5).getFirst();

            assertThat(produit.libelle()).isEqualTo("DOLIPRANE");
            assertThat(produit.montantRemise()).isEqualTo(120_000L);
            assertThat(produit.nbVentes()).isEqualTo(45);
        }
    }

    // ===== utilitaires =====

    private static Object[] periode(long ca, int nbTransactions, String panierMoyen, String tauxMarge) {
        return new Object[] { ca, nbTransactions, new BigDecimal(panierMoyen), new BigDecimal(tauxMarge) };
    }

    private static Object[] jour(LocalDate date, int nbTransactions, long caNet) {
        return new Object[] {
            java.sql.Date.valueOf(date),
            nbTransactions,
            caNet,
            caNet,
            BigDecimal.ZERO,
            0L,
            0L,
            BigDecimal.ZERO,
            nbTransactions,
            caNet,
            0L
        };
    }
}
