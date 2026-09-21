package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.service.dto.report.TiersPayantCreancesSummaryDTO;
import com.kobe.warehouse.service.dto.report.TiersPayantInvoiceDTO;
import com.kobe.warehouse.service.dto.report.TiersPayantInvoiceDTO.AgeCategory;
import com.kobe.warehouse.service.dto.report.TiersPayantInvoiceDTO.InvoiceStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les créances tiers payant sont l'argent que l'officine a avancé pour le compte des assureurs et
 * des mutuelles. C'est souvent son premier poste de trésorerie, et une facture oubliée trois mois
 * est une facture qu'on ne se souvient plus d'avoir émise.
 *
 * <p>L'écran la range donc par <b>ancienneté</b> : moins de trente jours, un à deux mois, deux à
 * trois, au-delà. Ce sont ces quatre tranches qu'on présente au tiers payant lors d'une relance, et
 * dont le total doit correspondre, au franc près, à ce qui reste dû.
 *
 * <p>Le piège est ailleurs, et il a déjà mordu : une facture porte <b>plusieurs bons</b>. Agréger
 * sans précaution retranche le montant déjà réglé autant de fois que la facture a de lignes, et les
 * tranches ressortent négatives. Le service agrège pour cette raison au niveau de la facture, ce que
 * ces tests vérifient.
 */
@DisplayName("TiersPayantReportService — créances et règlements des tiers payants")
class TiersPayantReportServiceIntegrationTest extends AbstractReportIntegrationTest {

    // ===== factures impayées =====

    @Nested
    @DisplayName("Factures impayées")
    class FacturesImpayees {

        @Test
        @DisplayName("reprend le montant facturé, le montant réglé et ce qui reste dû")
        void montantsDeLaFacture() {
            TiersPayant mutuelle = tiersPayantSeul("MUGEFCI");
            facture(mutuelle, ilYA(10), 500_000, 200_000, InvoiceStatut.PARTIALLY_PAID);
            viderLeCache();

            TiersPayantInvoiceDTO impayee = services.tiersPayantReportService.getUnpaidInvoices(null, null).getFirst();

            assertThat(impayee.tiersPayantLibelle()).startsWith("MUGEFCI");
            assertThat(impayee.montantFacture()).isEqualTo(500_000);
            assertThat(impayee.montantPaye()).isEqualTo(200_000);
            assertThat(impayee.montantRestant()).isEqualTo(300_000);
            assertThat(impayee.statut()).isEqualTo(InvoiceStatus.PARTIAL);
        }

        @Test
        @DisplayName("une facture soldée ne figure plus dans les impayés")
        void factureSoldeeExclue() {
            TiersPayant mutuelle = tiersPayantSeul("MUGEFCI");
            facture(mutuelle, ilYA(10), 500_000, 500_000, InvoiceStatut.PAID);
            viderLeCache();

            assertThat(services.tiersPayantReportService.getUnpaidInvoices(null, null)).isEmpty();
        }

        /**
         * Les quatre tranches sont testées sur leurs bornes : c'est ce découpage qu'on présente au
         * tiers payant lors d'une relance, et une facture rangée dans la mauvaise tranche affaiblit
         * l'argumentaire.
         */
        @Test
        @DisplayName("chaque facture est rangée dans sa tranche d'ancienneté")
        void trancheDAnciennete() {
            TiersPayant mutuelle = tiersPayantSeul("MUGEFCI");
            facture(mutuelle, ilYA(10), 100_000, 0, InvoiceStatut.NOT_PAID);
            facture(mutuelle, ilYA(45), 100_000, 0, InvoiceStatut.NOT_PAID);
            facture(mutuelle, ilYA(75), 100_000, 0, InvoiceStatut.NOT_PAID);
            facture(mutuelle, ilYA(120), 100_000, 0, InvoiceStatut.NOT_PAID);
            viderLeCache();

            assertThat(services.tiersPayantReportService.getUnpaidInvoices(null, null))
                .extracting(TiersPayantInvoiceDTO::ageCategory)
                .containsExactly(
                    AgeCategory.LESS_THAN_30,
                    AgeCategory.BETWEEN_30_60,
                    AgeCategory.BETWEEN_60_90,
                    AgeCategory.MORE_THAN_90
                );
        }

        @Test
        @DisplayName("l'ancienneté est comptée en jours depuis l'émission")
        void ancienneteEnJours() {
            TiersPayant mutuelle = tiersPayantSeul("MUGEFCI");
            facture(mutuelle, ilYA(45), 100_000, 0, InvoiceStatut.NOT_PAID);
            viderLeCache();

            assertThat(services.tiersPayantReportService.getUnpaidInvoices(null, null).getFirst().daysSinceInvoice()).isEqualTo(45);
        }

        @Test
        @DisplayName("les plus anciennes factures passent en tête")
        void ordreParAnciennete() {
            TiersPayant mutuelle = tiersPayantSeul("MUGEFCI");
            facture(mutuelle, ilYA(100), 100_000, 0, InvoiceStatut.NOT_PAID);
            facture(mutuelle, ilYA(5), 200_000, 0, InvoiceStatut.NOT_PAID);
            viderLeCache();

            assertThat(services.tiersPayantReportService.getUnpaidInvoices(null, null))
                .extracting(TiersPayantInvoiceDTO::montantFacture)
                .containsExactly(200_000, 100_000);
        }

        @Test
        @DisplayName("le filtre par tranche ne rend que les factures de cette ancienneté")
        void filtreParTranche() {
            TiersPayant mutuelle = tiersPayantSeul("MUGEFCI");
            facture(mutuelle, ilYA(10), 100_000, 0, InvoiceStatut.NOT_PAID);
            facture(mutuelle, ilYA(120), 900_000, 0, InvoiceStatut.NOT_PAID);
            viderLeCache();

            assertThat(services.tiersPayantReportService.getUnpaidInvoices(null, AgeCategory.MORE_THAN_90))
                .extracting(TiersPayantInvoiceDTO::montantFacture)
                .containsExactly(900_000);
            assertThat(services.tiersPayantReportService.getUnpaidInvoices(null, AgeCategory.LESS_THAN_30))
                .extracting(TiersPayantInvoiceDTO::montantFacture)
                .containsExactly(100_000);
        }

        @Test
        @DisplayName("le filtre par groupe ne rend que les factures de ce groupe")
        void filtreParGroupe() {
            GroupeTiersPayant groupe = groupeTiersPayant("RESEAU");
            facture(tiersPayant("AFFILIEE", groupe), ilYA(10), 100_000, 0, InvoiceStatut.NOT_PAID);
            facture(tiersPayantSeul("INDEPENDANTE"), ilYA(10), 900_000, 0, InvoiceStatut.NOT_PAID);
            viderLeCache();

            assertThat(services.tiersPayantReportService.getUnpaidInvoices(groupe.getId(), null))
                .extracting(TiersPayantInvoiceDTO::montantFacture)
                .containsExactly(100_000);
        }

        @Test
        @DisplayName("base vierge : aucune créance")
        void baseVierge() {
            assertThat(services.tiersPayantReportService.getUnpaidInvoices(null, null)).isEmpty();
        }
    }

    // ===== synthèse des créances =====

    @Nested
    @DisplayName("Synthèse des créances")
    class SyntheseDesCreances {

        @Test
        @DisplayName("ventile le reste dû de chaque tiers payant dans les quatre tranches")
        void ventilationParTranche() {
            TiersPayant mutuelle = tiersPayantSeul("MUGEFCI");
            facture(mutuelle, ilYA(10), 100_000, 0, InvoiceStatut.NOT_PAID);
            facture(mutuelle, ilYA(45), 200_000, 0, InvoiceStatut.NOT_PAID);
            facture(mutuelle, ilYA(75), 300_000, 0, InvoiceStatut.NOT_PAID);
            facture(mutuelle, ilYA(120), 400_000, 0, InvoiceStatut.NOT_PAID);
            viderLeCache();

            TiersPayantCreancesSummaryDTO synthese = services.tiersPayantReportService.getCreancesSummary().getFirst();

            assertThat(synthese.nombreFactures()).isEqualTo(4);
            assertThat(synthese.montantTotal()).isEqualTo(1_000_000);
            assertThat(synthese.montantMoinsDe30Jours()).isEqualTo(100_000);
            assertThat(synthese.montantEntre30Et60Jours()).isEqualTo(200_000);
            assertThat(synthese.montantEntre60Et90Jours()).isEqualTo(300_000);
            assertThat(synthese.montantPlusDe90Jours()).isEqualTo(400_000);
        }

        /**
         * Le règlement partiel n'est retranché qu'une fois, quel que soit le nombre de bons de la
         * facture — c'est précisément ce qui faisait ressortir des tranches négatives.
         */
        @Test
        @DisplayName("un règlement partiel n'est retranché qu'une seule fois")
        void reglementPartielRetrancheUneFois() {
            TiersPayant mutuelle = tiersPayantSeul("MUGEFCI");
            facture(mutuelle, ilYA(10), 500_000, 200_000, InvoiceStatut.PARTIALLY_PAID);
            viderLeCache();

            TiersPayantCreancesSummaryDTO synthese = services.tiersPayantReportService.getCreancesSummary().getFirst();

            assertThat(synthese.montantMoinsDe30Jours()).isEqualTo(300_000);
            assertThat(synthese.montantMoinsDe30Jours()).isNotNegative();
        }

        @Test
        @DisplayName("les tiers payants d'un même groupe sont regroupés")
        void regroupementParGroupe() {
            GroupeTiersPayant groupe = groupeTiersPayant("RESEAU");
            facture(tiersPayant("AFFILIEE A", groupe), ilYA(10), 100_000, 0, InvoiceStatut.NOT_PAID);
            facture(tiersPayant("AFFILIEE B", groupe), ilYA(10), 200_000, 0, InvoiceStatut.NOT_PAID);
            viderLeCache();

            List<TiersPayantCreancesSummaryDTO> synthese = services.tiersPayantReportService.getCreancesSummary();

            assertThat(synthese).hasSize(1);
            assertThat(synthese.getFirst().groupeTiersPayantLibelle()).startsWith("RESEAU");
            assertThat(synthese.getFirst().nombreFactures()).isEqualTo(2);
            assertThat(synthese.getFirst().montantTotal()).isEqualTo(300_000);
        }

        /** Un tiers payant sans groupe garde son propre nom : il ne doit pas se fondre dans les autres. */
        @Test
        @DisplayName("un tiers payant sans groupe apparaît sous son propre nom")
        void tiersPayantSansGroupe() {
            TiersPayant mutuelle = tiersPayantSeul("INDEPENDANTE");
            facture(mutuelle, ilYA(10), 100_000, 0, InvoiceStatut.NOT_PAID);
            viderLeCache();

            TiersPayantCreancesSummaryDTO synthese = services.tiersPayantReportService.getCreancesSummary().getFirst();

            assertThat(synthese.groupeTiersPayantId()).isNull();
            assertThat(synthese.groupeTiersPayantLibelle()).isEqualTo(mutuelle.getName());
        }

        @Test
        @DisplayName("les plus gros débiteurs passent en tête")
        void ordreParMontant() {
            facture(tiersPayantSeul("PETITE"), ilYA(10), 100_000, 0, InvoiceStatut.NOT_PAID);
            facture(tiersPayantSeul("GROSSE"), ilYA(10), 900_000, 0, InvoiceStatut.NOT_PAID);
            viderLeCache();

            assertThat(services.tiersPayantReportService.getCreancesSummary())
                .extracting(TiersPayantCreancesSummaryDTO::montantTotal)
                .containsExactly(900_000, 100_000);
        }

        @Test
        @DisplayName("une facture soldée sort de la synthèse")
        void factureSoldeeExclue() {
            TiersPayant mutuelle = tiersPayantSeul("MUGEFCI");
            facture(mutuelle, ilYA(10), 500_000, 500_000, InvoiceStatut.PAID);
            viderLeCache();

            assertThat(services.tiersPayantReportService.getCreancesSummary()).isEmpty();
        }

        @Test
        @DisplayName("base vierge : aucune créance à ventiler")
        void baseVierge() {
            assertThat(services.tiersPayantReportService.getCreancesSummary()).isEmpty();
        }
    }

    // ===== historique des règlements =====

    @Nested
    @DisplayName("Historique des règlements")
    class HistoriqueDesReglements {

        @Test
        @DisplayName("ne rend que les factures soldées de la période")
        void facturesSoldeesDeLaPeriode() {
            TiersPayant mutuelle = tiersPayantSeul("MUGEFCI");
            facture(mutuelle, ilYA(10), 500_000, 500_000, InvoiceStatut.PAID);
            facture(mutuelle, ilYA(10), 300_000, 0, InvoiceStatut.NOT_PAID);
            viderLeCache();

            List<TiersPayantInvoiceDTO> historique = services.tiersPayantReportService.getPaymentHistory(
                null,
                ilYA(30),
                LocalDate.now()
            );

            assertThat(historique).hasSize(1);
            assertThat(historique.getFirst().montantFacture()).isEqualTo(500_000);
            assertThat(historique.getFirst().statut()).isEqualTo(InvoiceStatus.PAID);
        }

        @Test
        @DisplayName("une facture soldée hors période n'apparaît pas")
        void factureHorsPeriodeExclue() {
            TiersPayant mutuelle = tiersPayantSeul("MUGEFCI");
            facture(mutuelle, ilYA(200), 500_000, 500_000, InvoiceStatut.PAID);
            viderLeCache();

            assertThat(services.tiersPayantReportService.getPaymentHistory(null, ilYA(30), LocalDate.now())).isEmpty();
        }

        @Test
        @DisplayName("le filtre par groupe s'applique aussi à l'historique")
        void filtreParGroupe() {
            GroupeTiersPayant groupe = groupeTiersPayant("RESEAU");
            facture(tiersPayant("AFFILIEE", groupe), ilYA(10), 100_000, 100_000, InvoiceStatut.PAID);
            facture(tiersPayantSeul("INDEPENDANTE"), ilYA(10), 900_000, 900_000, InvoiceStatut.PAID);
            viderLeCache();

            assertThat(services.tiersPayantReportService.getPaymentHistory(groupe.getId(), ilYA(30), LocalDate.now()))
                .extracting(TiersPayantInvoiceDTO::montantFacture)
                .containsExactly(100_000);
        }

        @Test
        @DisplayName("base vierge : aucun règlement")
        void baseVierge() {
            assertThat(services.tiersPayantReportService.getPaymentHistory(null, ilYA(30), LocalDate.now())).isEmpty();
        }
    }

    // ===== fabriques locales =====

    private static LocalDate ilYA(int jours) {
        return LocalDate.now().minusDays(jours);
    }

    private TiersPayant tiersPayantSeul(String nom) {
        return tiersPayant(nom, null);
    }
}
