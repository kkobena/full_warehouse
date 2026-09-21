package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.service.dto.enumeration.TypeVenteDTO;
import com.kobe.warehouse.service.dto.report.DailySalesSummaryDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La synthèse des ventes est la ligne du jour : combien de tickets, pour quel montant, avec quelle
 * remise consentie. C'est ce que le pharmacien regarde en fermant, et ce qui alimente les
 * déclarations.
 *
 * <p>Elle vient de {@code mv_daily_sales_summary}, qui ventile par jour et par <b>type de vente</b>
 * — le {@code dtype} : vente comptant, vente à un tiers payant, vente à un dépôt. Deux exclusions y
 * sont structurantes et se voient mal : une vente <b>annulée</b> ne doit pas gonfler la journée, et
 * une vente <b>importée</b> d'un ancien système n'a pas été faite ici — la compter reviendrait à
 * déclarer deux fois le même chiffre d'affaires.
 *
 * <p>Le <b>dépôt</b> est le cas délicat : ces ventes portent la catégorie {@code CA_DEPOT} parce
 * qu'elles sont un transfert et non du chiffre d'affaires déclaré. Elles étaient absentes de la vue
 * jusqu'en V2.0.4, alors que l'écran proposait le filtre correspondant — qui ne rendait donc rien.
 * Elles y sont désormais, distinguées ligne à ligne.
 */
@DisplayName("SalesSummaryReportService — synthèse des ventes sur mv_daily_sales_summary")
class SalesSummaryReportServiceIntegrationTest extends AbstractReportIntegrationTest {

    private static final String VUE = "mv_daily_sales_summary";

    // ===== agrégation =====

    @Nested
    @DisplayName("Agrégation de la journée")
    class Agregation {

        @Test
        @DisplayName("compte les tickets et totalise le chiffre d'affaires du jour")
        void totauxDuJour() {
            venteDe(30_000, 0);
            venteDe(50_000, 0);
            rafraichir(VUE);

            DailySalesSummaryDTO ligne = services.salesSummaryReportService.getDailySalesSummaryByDate(LocalDate.now()).getFirst();

            assertThat(ligne.saleDate()).isEqualTo(LocalDate.now());
            assertThat(ligne.nbVentes()).isEqualTo(2);
            assertThat(ligne.caTotal()).isEqualTo(80_000);
            assertThat(ligne.panierMoyen()).isEqualByComparingTo("40000");
        }

        /**
         * Le chiffre d'affaires net est celui d'après remise : c'est l'écart entre les deux qui dit
         * ce que l'officine a consenti dans la journée.
         */
        @Test
        @DisplayName("la remise consentie se lit dans l'écart entre le brut et le net")
        void remisesConsentiess() {
            venteDe(100_000, 15_000);
            rafraichir(VUE);

            DailySalesSummaryDTO ligne = services.salesSummaryReportService.getDailySalesSummaryByDate(LocalDate.now()).getFirst();

            assertThat(ligne.caTotal()).isEqualTo(100_000);
            assertThat(ligne.totalRemises()).isEqualTo(15_000);
            assertThat(ligne.caNet()).isEqualTo(85_000);
        }

        @Test
        @DisplayName("chaque type de vente forme sa propre ligne")
        void uneLigneParTypeDeVente() {
            venteDe(100_000, 0);
            venteDepot(LocalDate.now(), 40_000);
            rafraichir(VUE);

            List<DailySalesSummaryDTO> lignes = services.salesSummaryReportService.getDailySalesSummaryByDate(LocalDate.now());

            assertThat(lignes).extracting(DailySalesSummaryDTO::type).containsExactlyInAnyOrder("CashSale", "VenteDepot");
        }

        /** L'écran n'affiche pas le nom de la classe Java mais le code métier : VNO, VO, dépôt. */
        @Test
        @DisplayName("le type est traduit en code métier pour l'affichage")
        void typeTraduitEnCodeMetier() {
            venteDe(100_000, 0);
            rafraichir(VUE);

            assertThat(services.salesSummaryReportService.getDailySalesSummaryByDate(LocalDate.now()).getFirst().typeVente())
                .isEqualTo("VNO");
        }
    }

    // ===== exclusions =====

    @Nested
    @DisplayName("Exclusions")
    class Exclusions {

        @Test
        @DisplayName("une vente annulée ne gonfle pas la journée")
        void venteAnnuleeExclue() {
            venteDe(100_000, 0);
            vente(LocalDate.now(), 999_000, 0, true, false, CategorieChiffreAffaire.CA);
            rafraichir(VUE);

            DailySalesSummaryDTO ligne = services.salesSummaryReportService.getDailySalesSummaryByDate(LocalDate.now()).getFirst();

            assertThat(ligne.nbVentes()).isEqualTo(1);
            assertThat(ligne.caTotal()).isEqualTo(100_000);
        }

        /**
         * Une vente reprise d'un ancien système n'a pas été faite ici. La compter dans la synthèse
         * reviendrait à déclarer deux fois le même chiffre d'affaires.
         */
        @Test
        @DisplayName("une vente importée n'a pas été faite ici et ne compte pas")
        void venteImporteeExclue() {
            venteDe(100_000, 0);
            vente(LocalDate.now(), 999_000, 0, false, true, CategorieChiffreAffaire.CA);
            rafraichir(VUE);

            assertThat(services.salesSummaryReportService.getDailySalesSummaryByDate(LocalDate.now()).getFirst().nbVentes())
                .isEqualTo(1);
        }

        @Test
        @DisplayName("une vente marquée à ignorer reste hors de la synthèse")
        void venteAIgnorerExclue() {
            vente(LocalDate.now(), 100_000, 0, false, false, CategorieChiffreAffaire.TO_IGNORE);
            rafraichir(VUE);

            assertThat(services.salesSummaryReportService.getDailySalesSummaryByDate(LocalDate.now())).isEmpty();
        }

        @Test
        @DisplayName("base vierge : aucune ligne")
        void baseVierge() {
            rafraichir(VUE);

            assertThat(services.salesSummaryReportService.getDailySalesSummaryByDate(LocalDate.now())).isEmpty();
            assertThat(services.salesSummaryReportService.getDailySalesSummary(LocalDate.now().minusDays(7), LocalDate.now())).isEmpty();
        }
    }

    // ===== période et filtres =====

    @Nested
    @DisplayName("Période et filtres")
    class PeriodeEtFiltres {

        @Test
        @DisplayName("la période est bornée aux deux extrémités, incluses")
        void periodeIncluseAuxBornes() {
            venteDe(LocalDate.now().minusDays(3), 10_000);
            venteDe(LocalDate.now().minusDays(1), 20_000);
            venteDe(LocalDate.now(), 30_000);
            rafraichir(VUE);

            List<DailySalesSummaryDTO> dansLaPeriode = services.salesSummaryReportService.getDailySalesSummary(
                LocalDate.now().minusDays(3),
                LocalDate.now().minusDays(1)
            );

            assertThat(dansLaPeriode).extracting(DailySalesSummaryDTO::caTotal).containsExactly(20_000, 10_000);
        }

        /** Le jour le plus récent en tête : c'est l'ordre dans lequel on relit une semaine écoulée. */
        @Test
        @DisplayName("les jours sont rendus du plus récent au plus ancien")
        void ordreAntichronologique() {
            venteDe(LocalDate.now().minusDays(2), 10_000);
            venteDe(LocalDate.now(), 30_000);
            rafraichir(VUE);

            assertThat(services.salesSummaryReportService.getDailySalesSummary(LocalDate.now().minusDays(7), LocalDate.now()))
                .extracting(DailySalesSummaryDTO::saleDate)
                .containsExactly(LocalDate.now(), LocalDate.now().minusDays(2));
        }

        @Test
        @DisplayName("le filtre par type ne rend que ce type de vente")
        void filtreParType() {
            venteDe(100_000, 0);
            venteDepot(LocalDate.now(), 40_000);
            rafraichir(VUE);

            assertThat(
                services.salesSummaryReportService.getDailySalesSummaryByType(
                    LocalDate.now().minusDays(7),
                    LocalDate.now(),
                    TypeVenteDTO.VenteDepot
                )
            )
                .extracting(DailySalesSummaryDTO::caTotal)
                .containsExactly(40_000);

            assertThat(
                services.salesSummaryReportService.getDailySalesSummaryByType(
                    LocalDate.now().minusDays(7),
                    LocalDate.now(),
                    TypeVenteDTO.CashSale
                )
            )
                .extracting(DailySalesSummaryDTO::caTotal)
                .containsExactly(100_000);
        }

        @Test
        @DisplayName("un type sans vente rend une liste vide plutôt qu'une erreur")
        void typeSansVente() {
            venteDe(100_000, 0);
            rafraichir(VUE);

            assertThat(
                services.salesSummaryReportService.getDailySalesSummaryByType(
                    LocalDate.now().minusDays(7),
                    LocalDate.now(),
                    TypeVenteDTO.ThirdPartySales
                )
            ).isEmpty();
        }
    }

    // ===== fabriques locales =====

    private CashSale venteDe(int montant, int remise) {
        return vente(LocalDate.now(), montant, remise, false, false, CategorieChiffreAffaire.CA);
    }

    private CashSale venteDe(LocalDate date, int montant) {
        return vente(date, montant, 0, false, false, CategorieChiffreAffaire.CA);
    }

    private CashSale vente(
        LocalDate date,
        int montant,
        int remise,
        boolean annulee,
        boolean importee,
        CategorieChiffreAffaire categorie
    ) {
        CashSale vente = venteFermee(date, annulee, categorie);
        vente.setSalesAmount(montant);
        vente.setNetAmount(montant - remise);
        vente.setDiscountAmount(remise);
        vente.setAmountToBePaid(montant - remise);
        vente.setPayrollAmount(montant);
        vente.setImported(importee);
        em.flush();
        return vente;
    }
}
