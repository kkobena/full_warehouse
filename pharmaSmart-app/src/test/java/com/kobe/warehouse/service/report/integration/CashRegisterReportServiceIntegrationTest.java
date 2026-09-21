package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.dto.report.CashMovementDTO;
import com.kobe.warehouse.service.dto.report.DailyCashRegisterReportDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * L'état de caisse est un document de contrôle : il confronte ce que la caisse aurait dû contenir —
 * fonds d'ouverture plus encaissements — à ce que le caissier a effectivement compté en fermant.
 * L'écart des deux est ce sur quoi une officine demande des explications, et parfois engage la
 * responsabilité de quelqu'un.
 *
 * <p>Un rapport de contrôle se doit donc d'être exact au franc près. Trois choses le déterminent :
 * le <b>total encaissé</b>, qui ne doit compter chaque encaissement qu'une fois ; l'<b>heure de
 * fermeture</b>, qui atteste du moment du comptage ; et la <b>ventilation par mode de paiement</b>,
 * qui dit combien d'espèces devraient physiquement se trouver dans le tiroir.
 *
 * <p>L'historique des mouvements complète l'état : il permet de remonter d'un écart à la vente qui
 * l'explique.
 */
@DisplayName("CashRegisterReportService — état de caisse et mouvements")
class CashRegisterReportServiceIntegrationTest extends AbstractReportIntegrationTest {

    // ===== état journalier =====

    @Nested
    @DisplayName("État journalier")
    class EtatJournalier {

        @Test
        @DisplayName("reprend l'identité de la caisse, son titulaire et son fonds d'ouverture")
        void identiteDeLaCaisse() {
            caisse(50_000L, aujourdHuiA(8, 0), null);
            viderLeCache();

            DailyCashRegisterReportDTO etat = services.cashRegisterReportService.getDailyReport(LocalDate.now()).getFirst();

            assertThat(etat.caisseLibelle()).startsWith("Caisse ");
            assertThat(etat.userName()).isEqualTo("System System");
            assertThat(etat.openingBalance()).isEqualTo(50_000);
            assertThat(etat.openingDate()).isEqualTo(aujourdHuiA(8, 0));
            assertThat(etat.date()).isEqualTo(LocalDate.now());
        }

        /**
         * L'heure de fermeture atteste du moment du comptage : c'est elle qu'on oppose au caissier
         * en cas d'écart. Elle ne peut pas être celle de l'ouverture.
         */
        @Test
        @DisplayName("l'heure de fermeture est celle de la fermeture, pas celle de l'ouverture")
        void heureDeFermeture() {
            LocalDateTime ouverture = aujourdHuiA(8, 0);
            LocalDateTime fermeture = aujourdHuiA(19, 30);
            caisse(50_000L, ouverture, fermeture);
            viderLeCache();

            DailyCashRegisterReportDTO etat = services.cashRegisterReportService.getDailyReport(LocalDate.now()).getFirst();

            assertThat(etat.openingDate()).isEqualTo(ouverture);
            assertThat(etat.closingDate()).isEqualTo(fermeture);
        }

        @Test
        @DisplayName("une caisse encore ouverte n'est pas signalée close")
        void caisseOuverteNonClose() {
            caisse(50_000L, aujourdHuiA(8, 0), null);
            viderLeCache();

            assertThat(services.cashRegisterReportService.getDailyReport(LocalDate.now()).getFirst().isClosed()).isFalse();
        }

        /**
         * Le total encaissé est la base de tout le contrôle. Chaque encaissement doit y compter une
         * fois et une seule — l'inflater rendrait chaque caisse artificiellement manquante.
         */
        @Test
        @DisplayName("le total encaissé compte chaque encaissement une seule fois")
        void totalEncaisseSansDoublon() {
            CashRegister caisse = caisse(50_000L, aujourdHuiA(8, 0), null);
            encaissement(caisse, "CASH", 60_000);
            encaissement(caisse, "OM", 40_000);
            // Trois ventes du jour : elles ne doivent pas démultiplier les encaissements.
            venteDuJour(10_000);
            venteDuJour(20_000);
            venteDuJour(30_000);
            viderLeCache();

            DailyCashRegisterReportDTO etat = services.cashRegisterReportService.getDailyReport(LocalDate.now()).getFirst();

            assertThat(etat.totalSales()).isEqualTo(100_000);
            assertThat(etat.numberOfTransactions()).isEqualTo(3);
        }

        @Test
        @DisplayName("le solde attendu est le fonds d'ouverture augmenté des encaissements")
        void soldeAttendu() {
            CashRegister caisse = caisse(50_000L, aujourdHuiA(8, 0), null);
            encaissement(caisse, "CASH", 100_000);
            viderLeCache();

            assertThat(services.cashRegisterReportService.getDailyReport(LocalDate.now()).getFirst().expectedBalance())
                .isEqualTo(150_000);
        }

        /** L'écart n'a de sens qu'une fois la caisse comptée : tant qu'elle est ouverte, il vaut zéro. */
        @Test
        @DisplayName("l'écart n'est chiffré qu'une fois la caisse fermée")
        void ecartSeulementSiFermee() {
            CashRegister ouverte = caisse(50_000L, aujourdHuiA(8, 0), null);
            encaissement(ouverte, "CASH", 100_000);
            viderLeCache();

            assertThat(services.cashRegisterReportService.getDailyReport(LocalDate.now()).getFirst().discrepancy()).isZero();
        }

        @Test
        @DisplayName("la ventilation par mode de paiement dit ce qui devrait être dans le tiroir")
        void ventilationParMode() {
            CashRegister caisse = caisse(0L, aujourdHuiA(8, 0), null);
            encaissement(caisse, "CASH", 100_000);
            encaissement(caisse, "OM", 40_000);
            viderLeCache();

            List<DailyCashRegisterReportDTO.PaymentModeBreakdown> ventilation = services.cashRegisterReportService
                .getDailyReport(LocalDate.now())
                .getFirst()
                .paymentModeBreakdowns();

            assertThat(ventilation)
                .extracting(
                    DailyCashRegisterReportDTO.PaymentModeBreakdown::modePaiement,
                    DailyCashRegisterReportDTO.PaymentModeBreakdown::amount
                )
                .containsExactly(
                    org.assertj.core.groups.Tuple.tuple("ESPECE", 100_000),
                    org.assertj.core.groups.Tuple.tuple("ORANGE", 40_000)
                );
        }

        @Test
        @DisplayName("une caisse ouverte un autre jour n'apparaît pas")
        void caisseDUnAutreJourExclue() {
            caisse(50_000L, LocalDate.now().minusDays(1).atTime(8, 0), null);
            viderLeCache();

            assertThat(services.cashRegisterReportService.getDailyReport(LocalDate.now())).isEmpty();
        }

        @Test
        @DisplayName("base vierge : aucun état de caisse")
        void baseVierge() {
            assertThat(services.cashRegisterReportService.getDailyReport(LocalDate.now())).isEmpty();
        }
    }

    // ===== synthèse sur une période =====

    @Nested
    @DisplayName("Synthèse sur une période")
    class Synthese {

        @Test
        @DisplayName("la synthèse enchaîne les états de chaque jour de la période")
        void enchainementDesJours() {
            caisse(10_000L, LocalDate.now().minusDays(2).atTime(8, 0), null);
            caisse(20_000L, LocalDate.now().minusDays(1).atTime(8, 0), null);
            caisse(30_000L, aujourdHuiA(8, 0), null);
            viderLeCache();

            List<DailyCashRegisterReportDTO> synthese = services.cashRegisterReportService.getCashRegisterSummary(
                LocalDate.now().minusDays(2),
                LocalDate.now()
            );

            assertThat(synthese).extracting(DailyCashRegisterReportDTO::openingBalance).containsExactly(10_000, 20_000, 30_000);
        }

        @Test
        @DisplayName("une période sans caisse rend une synthèse vide")
        void periodeSansCaisse() {
            assertThat(
                services.cashRegisterReportService.getCashRegisterSummary(LocalDate.now().minusDays(7), LocalDate.now())
            ).isEmpty();
        }
    }

    // ===== historique des mouvements =====

    @Nested
    @DisplayName("Historique des mouvements")
    class Mouvements {

        /** C'est ce qui permet de remonter d'un écart de caisse à la vente qui l'explique. */
        @Test
        @DisplayName("chaque vente de la période donne un mouvement")
        void mouvementsDeLaPeriode() {
            caisse(0L, aujourdHuiA(8, 0), null);
            venteDuJour(30_000);
            venteDuJour(20_000);
            viderLeCache();

            List<CashMovementDTO> mouvements = services.cashRegisterReportService.getCashMovements(
                LocalDate.now(),
                LocalDate.now(),
                null,
                null
            );

            assertThat(mouvements).hasSize(2);
            assertThat(mouvements).extracting(CashMovementDTO::movementType).containsOnly("VENTE");
            assertThat(mouvements).extracting(CashMovementDTO::amount).containsExactlyInAnyOrder(30_000, 20_000);
        }

        @Test
        @DisplayName("le mouvement porte le numéro de reçu et le nom du client")
        void identiteDuMouvement() {
            caisse(0L, aujourdHuiA(8, 0), null);
            var client = client("KOUASSI", "Jean", "0102030405");
            var vente = venteFermee(LocalDate.now(), false, CategorieChiffreAffaire.CA, client);
            vente.setSalesAmount(25_000);
            em.flush();
            viderLeCache();

            CashMovementDTO mouvement = services.cashRegisterReportService
                .getCashMovements(LocalDate.now(), LocalDate.now(), null, null)
                .getFirst();

            assertThat(mouvement.saleNumber()).isNotBlank();
            assertThat(mouvement.customerName()).isEqualTo("Jean KOUASSI");
            assertThat(mouvement.transactionDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("une vente anonyme est signalée comme telle")
        void venteAnonyme() {
            caisse(0L, aujourdHuiA(8, 0), null);
            venteDuJour(25_000);
            viderLeCache();

            assertThat(
                services.cashRegisterReportService.getCashMovements(LocalDate.now(), LocalDate.now(), null, null).getFirst().customerName()
            ).isEqualTo("Client comptant");
        }

        @Test
        @DisplayName("le filtre par utilisateur ne retient que ses mouvements")
        void filtreParUtilisateur() {
            caisse(0L, aujourdHuiA(8, 0), null);
            venteDuJour(25_000);
            viderLeCache();

            assertThat(
                services.cashRegisterReportService.getCashMovements(
                    LocalDate.now(),
                    LocalDate.now(),
                    utilisateur.getId().longValue(),
                    null
                )
            ).hasSize(1);
            assertThat(services.cashRegisterReportService.getCashMovements(LocalDate.now(), LocalDate.now(), 999_999L, null)).isEmpty();
        }

        @Test
        @DisplayName("une vente hors période n'apparaît pas")
        void ventehorsPeriodeExclue() {
            caisse(0L, aujourdHuiA(8, 0), null);
            vente(LocalDate.now().minusDays(10), 25_000);
            viderLeCache();

            assertThat(services.cashRegisterReportService.getCashMovements(LocalDate.now(), LocalDate.now(), null, null)).isEmpty();
        }

        @Test
        @DisplayName("base vierge : aucun mouvement")
        void baseVierge() {
            assertThat(
                services.cashRegisterReportService.getCashMovements(LocalDate.now().minusDays(7), LocalDate.now(), null, null)
            ).isEmpty();
        }
    }

    // ===== fabriques locales =====

    private static LocalDateTime aujourdHuiA(int heure, int minute) {
        return LocalDate.now().atTime(heure, minute);
    }

    private CashRegister caisse(long fondDeCaisse, LocalDateTime ouverture, LocalDateTime fermeture) {
        CashRegister caisse = caisseOuverte();
        caisse.setInitAmount(fondDeCaisse);
        caisse.setBeginTime(ouverture);
        caisse.setEndTime(fermeture);
        if (fermeture != null) {
            caisse.setStatut(com.kobe.warehouse.domain.enumeration.CashRegisterStatut.CLOSED);
        }
        em.flush();
        return caisse;
    }

    private void encaissement(CashRegister caisse, String codeMode, long montant) {
        var item = new com.kobe.warehouse.domain.CashRegisterItem();
        item.setCashRegister(caisse);
        item.setPaymentMode(em.find(com.kobe.warehouse.domain.PaymentMode.class, codeMode));
        item.setTypeFinancialTransaction(TypeFinancialTransaction.CASH_SALE);
        item.setAmount(montant);
        em.persist(item);
        em.flush();
    }

    private void venteDuJour(int montant) {
        vente(LocalDate.now(), montant);
    }

    private void vente(LocalDate date, int montant) {
        var vente = venteFermee(date, false, CategorieChiffreAffaire.CA);
        vente.setSalesAmount(montant);
        vente.setNetAmount(montant);
        vente.setAmountToBePaid(montant);
        vente.setPayrollAmount(montant);
        em.flush();
    }
}
