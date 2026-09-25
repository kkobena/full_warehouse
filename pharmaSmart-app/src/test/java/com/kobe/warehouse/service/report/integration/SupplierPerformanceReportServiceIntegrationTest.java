package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.service.dto.report.SupplierPerformanceDTO;
import com.kobe.warehouse.service.dto.report.SupplierPerformanceSummaryDTO;
import com.kobe.warehouse.service.dto.report.SupplierPurchaseDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La performance fournisseur est ce sur quoi l'acheteur s'appuie pour arbitrer entre deux
 * grossistes : combien on lui achète, en combien de jours il livre, et s'il livre bien ce qu'on a
 * commandé. Les trois se calculent dans {@code mv_supplier_performance}, une vue matérialisée
 * rafraîchie par un job — donc un objet qui ne se met pas à jour tout seul, et dont le contenu ne
 * ressemble à rien tant qu'on ne l'a pas recalculé.
 *
 * <p>Ce que ces tests tiennent, c'est l'accord entre la <b>formule</b> et ce que le service en
 * rapporte. Le <b>délai</b> se lit sur l'écart entre la date de commande et celle de réception ; la
 * <b>conformité</b> sur le rapport du reçu au demandé, et c'est la seule alerte qui dise qu'un
 * fournisseur sert systématiquement moins que commandé. La <b>note globale</b> combine les trois
 * avec des poids fixés dans la vue : elle décide du classement, donc de qui reçoit la prochaine
 * commande.
 *
 * <p>Le périmètre compte autant : la vue ne retient qu'un fournisseur ayant réellement livré dans
 * l'année. Un fournisseur référencé mais jamais servi n'a pas de performance à montrer, et l'y
 * faire apparaître avec des zéros le ferait passer pour mauvais.
 */
@DisplayName("SupplierPerformanceReportService — performance fournisseur sur mv_supplier_performance")
class SupplierPerformanceReportServiceIntegrationTest extends AbstractReportIntegrationTest {

    private static final String VUE = "mv_supplier_performance";

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre")
    class Perimetre {

        @Test
        @DisplayName("un fournisseur sans réception dans l'année n'a pas de performance")
        void fournisseurSansReceptionAbsent() {
            fournisseur("JAMAIS SERVI " + unique(""));
            rafraichir(VUE);

            assertThat(services.supplierPerformanceReportService.getAllSupplierPerformance()).isEmpty();
        }

        @Test
        @DisplayName("une réception d'il y a plus d'un an ne compte plus")
        void receptionHorsFenetreIgnoree() {
            Fournisseur laborex = fournisseur("LABOREX " + unique(""));
            reception(laborex, LocalDate.now().minusMonths(15), LocalDate.now().minusMonths(15).plusDays(3), 1_000_000, 10, 10);
            rafraichir(VUE);

            assertThat(services.supplierPerformanceReportService.getAllSupplierPerformance()).isEmpty();
        }

        @Test
        @DisplayName("base vierge : aucune ligne, et une synthèse à zéro")
        void baseVierge() {
            rafraichir(VUE);

            assertThat(services.supplierPerformanceReportService.getAllSupplierPerformance()).isEmpty();

            SupplierPerformanceSummaryDTO synthese = services.supplierPerformanceReportService.getSupplierPerformanceSummary();

            assertThat(synthese.totalSuppliers()).isZero();
            assertThat(synthese.totalPurchaseAmountLast12Months()).isZero();
            assertThat(synthese.avgDeliveryDays()).isZero();
        }

        @Test
        @DisplayName("un fournisseur inconnu ne rend rien plutôt que de lever une erreur")
        void fournisseurInconnu() {
            rafraichir(VUE);

            assertThat(services.supplierPerformanceReportService.getSupplierPerformance(999_999)).isNull();
        }
    }

    // ===== indicateurs =====

    @Nested
    @DisplayName("Indicateurs")
    class Indicateurs {

        @Test
        @DisplayName("le volume d'achat de l'année totalise les réceptions")
        void volumeAnnuel() {
            Fournisseur laborex = fournisseur("LABOREX " + unique(""));
            reception(laborex, LocalDate.now().minusDays(10), LocalDate.now().minusDays(7), 3_000_000, 10, 10);
            reception(laborex, LocalDate.now().minusDays(40), LocalDate.now().minusDays(37), 2_000_000, 10, 10);
            rafraichir(VUE);

            SupplierPerformanceDTO resultat = services.supplierPerformanceReportService.getSupplierPerformance(laborex.getId());

            assertThat(resultat.nbOrdersLast12Months()).isEqualTo(2);
            assertThat(resultat.purchaseAmountLast12Months()).isEqualTo(5_000_000L);
        }

        /** Le mois écoulé se lit séparément : c'est lui qui montre un fournisseur qu'on vient d'arrêter. */
        @Test
        @DisplayName("le mois écoulé se compte à part de l'année")
        void volumeDuMois() {
            Fournisseur laborex = fournisseur("LABOREX " + unique(""));
            reception(laborex, LocalDate.now().minusDays(10), LocalDate.now().minusDays(7), 3_000_000, 10, 10);
            reception(laborex, LocalDate.now().minusDays(40), LocalDate.now().minusDays(37), 2_000_000, 10, 10);
            rafraichir(VUE);

            SupplierPerformanceDTO resultat = services.supplierPerformanceReportService.getSupplierPerformance(laborex.getId());

            assertThat(resultat.nbOrdersLast30Days()).isEqualTo(1);
            assertThat(resultat.purchaseAmountLast30Days()).isEqualTo(3_000_000L);
        }

        @Test
        @DisplayName("le délai de livraison est l'écart entre la commande et la réception")
        void delaiDeLivraison() {
            Fournisseur laborex = fournisseur("LABOREX " + unique(""));
            reception(laborex, LocalDate.now().minusDays(10), LocalDate.now().minusDays(8), 1_000_000, 10, 10);
            reception(laborex, LocalDate.now().minusDays(20), LocalDate.now().minusDays(16), 1_000_000, 10, 10);
            rafraichir(VUE);

            SupplierPerformanceDTO resultat = services.supplierPerformanceReportService.getSupplierPerformance(laborex.getId());

            assertThat(resultat.minDeliveryDays()).isEqualTo(2);
            assertThat(resultat.maxDeliveryDays()).isEqualTo(4);
            assertThat(resultat.avgDeliveryDays()).isEqualTo(3);
        }

        /**
         * Servir soixante-dix unités sur cent commandées n'est pas un retard, c'est un manque :
         * c'est ce que la conformité met en évidence, et rien d'autre ne le dit.
         */
        @Test
        @DisplayName("la conformité rapporte le reçu au demandé")
        void conformite() {
            Fournisseur laborex = fournisseur("LABOREX " + unique(""));
            reception(laborex, LocalDate.now().minusDays(10), LocalDate.now().minusDays(7), 1_000_000, 100, 70);
            rafraichir(VUE);

            SupplierPerformanceDTO resultat = services.supplierPerformanceReportService.getSupplierPerformance(laborex.getId());

            assertThat(resultat.conformityRatePct()).isEqualByComparingTo("70.00");
        }

        /**
         * La note combine volume, délai et conformité avec les poids 40 / 30 / 30 fixés dans la
         * vue. Le jeu d'essai vise un cas exact : dix millions d'achat (volume au plafond), trois
         * jours de délai, conformité parfaite → 40 + 27 + 30.
         */
        @Test
        @DisplayName("la note combine volume, délai et conformité")
        void noteGlobale() {
            Fournisseur laborex = fournisseur("LABOREX " + unique(""));
            reception(laborex, LocalDate.now().minusDays(10), LocalDate.now().minusDays(7), 10_000_000, 100, 100);
            rafraichir(VUE);

            SupplierPerformanceDTO resultat = services.supplierPerformanceReportService.getSupplierPerformance(laborex.getId());

            assertThat(resultat.performanceScore()).isEqualByComparingTo("97.00");
        }

        @Test
        @DisplayName("l'identité et les coordonnées du fournisseur accompagnent la performance")
        void identiteReportee() {
            Fournisseur laborex = fournisseur("LABOREX " + unique(""));
            reception(laborex, LocalDate.now().minusDays(10), LocalDate.now().minusDays(7), 1_000_000, 10, 10);
            rafraichir(VUE);

            SupplierPerformanceDTO resultat = services.supplierPerformanceReportService.getSupplierPerformance(laborex.getId());

            assertThat(resultat.fournisseurName()).startsWith("LABOREX");
            assertThat(resultat.fournisseurCode()).isEqualTo(laborex.getCode());
            assertThat(resultat.phone()).isEqualTo("0100000000");
            assertThat(resultat.mobile()).isEqualTo("0700000000");
        }
    }

    // ===== classements =====

    @Nested
    @DisplayName("Classements")
    class Classements {

        @Test
        @DisplayName("le classement par volume met le plus gros fournisseur en tête")
        void classementParVolume() {
            Fournisseur gros = fournisseur("GROS " + unique(""));
            Fournisseur petit = fournisseur("PETIT " + unique(""));
            reception(gros, LocalDate.now().minusDays(10), LocalDate.now().minusDays(7), 9_000_000, 10, 10);
            reception(petit, LocalDate.now().minusDays(10), LocalDate.now().minusDays(7), 1_000_000, 10, 10);
            rafraichir(VUE);

            List<SupplierPerformanceDTO> classement = services.supplierPerformanceReportService.getTopSuppliersByVolume(5);

            assertThat(classement).extracting(SupplierPerformanceDTO::fournisseurName).containsExactly(gros.getLibelle(), petit.getLibelle());
        }

        @Test
        @DisplayName("la limite du classement est respectée")
        void limiteDuClassement() {
            for (int i = 0; i < 3; i++) {
                reception(fournisseur("FRS " + unique("")), LocalDate.now().minusDays(10), LocalDate.now().minusDays(7), 1_000_000, 10, 10);
            }
            rafraichir(VUE);

            assertThat(services.supplierPerformanceReportService.getTopSuppliersByVolume(2)).hasSize(2);
        }

        @Test
        @DisplayName("la liste complète est triée par note décroissante")
        void listeTrieeParNote() {
            Fournisseur bon = fournisseur("BON " + unique(""));
            Fournisseur mauvais = fournisseur("MAUVAIS " + unique(""));
            reception(bon, LocalDate.now().minusDays(10), LocalDate.now().minusDays(9), 10_000_000, 100, 100);
            reception(mauvais, LocalDate.now().minusDays(10), LocalDate.now().minusDays(0), 100_000, 100, 50);
            rafraichir(VUE);

            List<SupplierPerformanceDTO> tous = services.supplierPerformanceReportService.getAllSupplierPerformance();

            assertThat(tous).extracting(SupplierPerformanceDTO::fournisseurName).containsExactly(bon.getLibelle(), mauvais.getLibelle());
        }

        @Test
        @DisplayName("le seuil de note écarte les fournisseurs en dessous")
        void seuilDeNote() {
            Fournisseur bon = fournisseur("BON " + unique(""));
            Fournisseur mauvais = fournisseur("MAUVAIS " + unique(""));
            reception(bon, LocalDate.now().minusDays(10), LocalDate.now().minusDays(9), 10_000_000, 100, 100);
            reception(mauvais, LocalDate.now().minusDays(10), LocalDate.now(), 100_000, 100, 50);
            rafraichir(VUE);

            assertThat(services.supplierPerformanceReportService.getSuppliersByPerformanceScore(90.0))
                .extracting(SupplierPerformanceDTO::fournisseurName)
                .containsExactly(bon.getLibelle());
        }

        /**
         * L'alerte se déclenche sur l'un ou l'autre défaut : livrer tard, ou livrer incomplet. Un
         * fournisseur ponctuel mais qui sert partiellement doit apparaître au même titre.
         */
        @Test
        @DisplayName("les fournisseurs en défaut de délai ou de conformité sont signalés")
        void defautsDeLivraison() {
            Fournisseur irreprochable = fournisseur("IRREPROCHABLE " + unique(""));
            Fournisseur lent = fournisseur("LENT " + unique(""));
            Fournisseur incomplet = fournisseur("INCOMPLET " + unique(""));
            reception(irreprochable, LocalDate.now().minusDays(10), LocalDate.now().minusDays(9), 1_000_000, 100, 100);
            reception(lent, LocalDate.now().minusDays(30), LocalDate.now().minusDays(10), 1_000_000, 100, 100);
            reception(incomplet, LocalDate.now().minusDays(10), LocalDate.now().minusDays(9), 1_000_000, 100, 60);
            rafraichir(VUE);

            assertThat(services.supplierPerformanceReportService.getSuppliersWithDeliveryIssues())
                .extracting(SupplierPerformanceDTO::fournisseurName)
                .containsExactlyInAnyOrder(lent.getLibelle(), incomplet.getLibelle());
        }
    }

    // ===== achats sur une fenêtre quelconque =====

    /**
     * {@code mv_supplier_performance} ne porte que deux fenêtres, trente jours et douze mois, écrites
     * en dur dans sa définition. Le tableau de bord, lui, se règle sur cinq périodes : il lui faut
     * donc un calcul direct sur les commandes. Ce que ces tests tiennent, c'est que la fenêtre
     * demandée soit bien celle qui borne le montant ET le classement -- c'est l'écart entre les deux
     * qui mettait en tête un fournisseur moins gros que le second.
     */
    @Nested
    @DisplayName("Achats sur une fenêtre quelconque")
    class AchatsSurLaPeriode {

        @Test
        @DisplayName("la fenêtre demandée borne les achats")
        void fenetreBorneLesAchats() {
            Fournisseur laborex = fournisseur("LABOREX " + unique(""));
            reception(laborex, LocalDate.now().minusDays(3), LocalDate.now().minusDays(1), 3_000_000, 10, 10);
            reception(laborex, LocalDate.now().minusDays(60), LocalDate.now().minusDays(57), 8_000_000, 10, 10);
            em.flush();

            List<SupplierPurchaseDTO> achats = services.supplierPerformanceReportService.getTopSuppliersByPeriode(
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                5
            );

            assertThat(achats).hasSize(1);
            assertThat(achats.getFirst().montantAchat()).isEqualTo(3_000_000L);
            assertThat(achats.getFirst().nbCommandes()).isEqualTo(1);
        }

        /**
         * Le gros fournisseur de l'année n'est pas forcément celui de la semaine. Le bloc affichait
         * les rangs de la fenêtre douze mois sous des montants de trente jours : le premier pouvait
         * donc afficher moins que le deuxième.
         */
        @Test
        @DisplayName("le classement suit la fenêtre, pas le volume annuel")
        void classementSuitLaFenetre() {
            Fournisseur grosSurLAnnee = fournisseur("ANNUEL " + unique(""));
            Fournisseur grosCetteSemaine = fournisseur("RECENT " + unique(""));
            reception(grosSurLAnnee, LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(6).plusDays(2), 9_000_000, 10, 10);
            reception(grosSurLAnnee, LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), 500_000, 10, 10);
            reception(grosCetteSemaine, LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), 2_000_000, 10, 10);
            em.flush();

            List<SupplierPurchaseDTO> achats = services.supplierPerformanceReportService.getTopSuppliersByPeriode(
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                5
            );

            assertThat(achats)
                .extracting(SupplierPurchaseDTO::fournisseurName)
                .containsExactly(grosCetteSemaine.getLibelle(), grosSurLAnnee.getLibelle());
        }

        @Test
        @DisplayName("les commandes d'un même fournisseur se cumulent sur la fenêtre")
        void commandesCumulees() {
            Fournisseur laborex = fournisseur("LABOREX " + unique(""));
            reception(laborex, LocalDate.now().minusDays(5), LocalDate.now().minusDays(4), 1_000_000, 10, 10);
            reception(laborex, LocalDate.now().minusDays(3), LocalDate.now().minusDays(2), 2_500_000, 10, 10);
            em.flush();

            List<SupplierPurchaseDTO> achats = services.supplierPerformanceReportService.getTopSuppliersByPeriode(
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                5
            );

            assertThat(achats).hasSize(1);
            assertThat(achats.getFirst().nbCommandes()).isEqualTo(2);
            assertThat(achats.getFirst().montantAchat()).isEqualTo(3_500_000L);
        }

        /**
         * Le score et le délai restent ceux de la vue : ce sont des indicateurs de qualité sur douze
         * mois, qu'une fenêtre d'un jour ne saurait mesurer. Ils accompagnent donc le montant de la
         * période sans être recalculés sur elle.
         */
        @Test
        @DisplayName("le score et le délai restent ceux de la vue douze mois")
        void qualiteVenueDeLaVue() {
            Fournisseur laborex = fournisseur("LABOREX " + unique(""));
            reception(laborex, LocalDate.now().minusDays(10), LocalDate.now().minusDays(7), 10_000_000, 100, 100);
            reception(laborex, LocalDate.now(), LocalDate.now(), 1_000_000, 100, 100);
            rafraichir(VUE);

            List<SupplierPurchaseDTO> achats = services.supplierPerformanceReportService.getTopSuppliersByPeriode(
                LocalDate.now(),
                LocalDate.now(),
                5
            );

            assertThat(achats).hasSize(1);
            assertThat(achats.getFirst().montantAchat()).isEqualTo(1_000_000L);
            assertThat(achats.getFirst().avgDeliveryDays()).isEqualTo(2);
            assertThat(achats.getFirst().performanceScore()).isPositive();
        }

        @Test
        @DisplayName("la limite du classement est respectée")
        void limiteRespectee() {
            for (int i = 0; i < 3; i++) {
                reception(fournisseur("FRS " + unique("")), LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), 1_000_000, 10, 10);
            }
            em.flush();

            assertThat(
                services.supplierPerformanceReportService.getTopSuppliersByPeriode(LocalDate.now().minusDays(7), LocalDate.now(), 2)
            ).hasSize(2);
        }

        @Test
        @DisplayName("une fenêtre sans achat ne rend rien")
        void fenetreVide() {
            reception(fournisseur("LABOREX " + unique("")), LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), 1_000_000, 10, 10);
            em.flush();

            assertThat(
                services.supplierPerformanceReportService.getTopSuppliersByPeriode(
                    LocalDate.now().minusMonths(6),
                    LocalDate.now().minusMonths(5),
                    5
                )
            ).isEmpty();
        }
    }

    // ===== synthèse =====

    @Nested
    @DisplayName("Synthèse")
    class Synthese {

        @Test
        @DisplayName("la synthèse totalise les fournisseurs, les achats et répartit les notes")
        void totaux() {
            Fournisseur bon = fournisseur("BON " + unique(""));
            Fournisseur mauvais = fournisseur("MAUVAIS " + unique(""));
            reception(bon, LocalDate.now().minusDays(10), LocalDate.now().minusDays(9), 10_000_000, 100, 100);
            reception(mauvais, LocalDate.now().minusDays(10), LocalDate.now(), 100_000, 100, 50);
            rafraichir(VUE);

            SupplierPerformanceSummaryDTO synthese = services.supplierPerformanceReportService.getSupplierPerformanceSummary();

            assertThat(synthese.totalSuppliers()).isEqualTo(2);
            assertThat(synthese.totalPurchaseAmountLast12Months()).isEqualTo(10_100_000L);
            assertThat(synthese.totalOrdersLast12Months()).isEqualTo(2);
            assertThat(synthese.suppliersWithGoodPerformance()).isEqualTo(1);
            assertThat(synthese.suppliersWithPoorPerformance()).isEqualTo(1);
        }
    }
}
