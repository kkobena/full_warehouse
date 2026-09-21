package com.kobe.warehouse.service.mobile.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.repository.MobileAlertRepository;
import com.kobe.warehouse.repository.MobileAlertRepository.ExpiryAlertProjection;
import com.kobe.warehouse.repository.MobileAlertRepository.OverdueInvoiceProjection;
import com.kobe.warehouse.repository.MobileAlertRepository.StockRuptureProjection;
import com.kobe.warehouse.repository.impl.MobileAlertRepositoryImpl;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Les alertes de l'écran d'accueil sont annoncées deux fois : par un compteur dans le bandeau, et
 * par la liste qu'on obtient en tapant dessus. Ce sont deux requêtes distinctes, et rien dans le
 * code ne les oblige à porter sur la même population.
 *
 * <p>Deux d'entre elles divergeaient. Les <b>péremptions</b> se comptaient par produit et se
 * listaient par lot : un produit à trois lots proches annonçait « 1 » et en affichait trois. Les
 * <b>impayés</b> se comptaient toutes factures confondues mais ne se listaient qu'à travers une
 * jointure fermée sur le groupe de tiers payant : les factures des tiers payants isolés — l'essentiel
 * d'une officine — étaient comptées et jamais montrées. Dans les deux cas le pharmacien voyait un
 * nombre auquel la liste ne répondait pas, sans qu'aucune erreur ne soit levée.
 */
@DisplayName("MobileAlertRepository — alertes lues sur PostgreSQL")
class MobileAlertRepositoryIntegrationTest extends AbstractMobileIntegrationTest {

    private MobileAlertRepository repository;

    @BeforeEach
    void cablerLeRepository() {
        MobileAlertRepositoryImpl impl = new MobileAlertRepositoryImpl();
        ReflectionTestUtils.setField(impl, "entityManager", em);
        repository = impl;
    }

    // ===== ruptures de stock =====

    @Nested
    @DisplayName("Ruptures de stock")
    class RupturesDeStock {

        @Test
        @DisplayName("un produit sans stock est une rupture")
        void produitSansStock() {
            int ecart = ecart(repository::getStockRuptureCount, () -> produitEnStock(unique("RUPTURE"), 0));

            assertThat(ecart).isEqualTo(1);
        }

        @Test
        @DisplayName("un produit approvisionné n'est pas une rupture")
        void produitApprovisionne() {
            int ecart = ecart(repository::getStockRuptureCount, () -> produitEnStock(unique("DISPO"), 12));

            assertThat(ecart).isZero();
        }

        @Test
        @DisplayName("la rupture désigne le produit et son code CIP")
        void detailDeLaRupture() {
            Produit produit = produitEnStock("DOLIPRANE MOBILE", 0);

            StockRuptureProjection rupture = repository
                .getStockRuptureAlerts()
                .stream()
                .filter(p -> p.productId() == produit.getId())
                .findFirst()
                .orElseThrow();

            assertThat(rupture.productName()).isEqualTo("DOLIPRANE MOBILE");
            assertThat(rupture.totalQtyStock()).isZero();
            assertThat(rupture.totalQtyUg()).isZero();
        }

        /** Le compteur du bandeau et la liste qu'il annonce doivent porter sur les mêmes produits. */
        @Test
        @DisplayName("le compteur annonce exactement ce que la liste contient")
        void compteurEtListeConcordent() {
            produitEnStock(unique("RUPTURE"), 0);
            produitEnStock(unique("RUPTURE"), 0);
            produitEnStock(unique("DISPO"), 5);
            em.flush();

            assertThat(repository.getStockRuptureCount()).isEqualTo(repository.getStockRuptureAlerts().size());
        }
    }

    // ===== péremptions =====

    @Nested
    @DisplayName("Péremptions")
    class Peremptions {

        @Test
        @DisplayName("un lot périmant dans la fenêtre est signalé")
        void lotDansLaFenetre() {
            Produit produit = produitEnStock(unique("PEREMPTION"), 10);
            lot(produit, LocalDate.now().plusDays(10), 5);
            em.flush();

            ExpiryAlertProjection alerte = alerteDuProduit(produit);

            assertThat(alerte.currentQuantity()).isEqualTo(5);
            assertThat(alerte.daysUntilExpiry()).isEqualTo(10);
            assertThat(alerte.expiryDate()).isEqualTo(LocalDate.now().plusDays(10));
        }

        @Test
        @DisplayName("un lot périmant au-delà de la fenêtre est ignoré")
        void lotHorsFenetre() {
            int ecart = ecart(() -> repository.getExpiringProductsCount(30), () -> {
                Produit produit = produitEnStock(unique("LOINTAIN"), 10);
                lot(produit, LocalDate.now().plusDays(45), 5);
            });

            assertThat(ecart).isZero();
        }

        /** Un lot épuisé ne périme plus rien : il n'y a plus de marchandise à écouler. */
        @Test
        @DisplayName("un lot épuisé est ignoré")
        void lotEpuise() {
            int ecart = ecart(() -> repository.getExpiringProductsCount(30), () -> {
                Produit produit = produitEnStock(unique("EPUISE"), 10);
                lot(produit, LocalDate.now().plusDays(10), 0);
            });

            assertThat(ecart).isZero();
        }

        /**
         * Le défaut que ce test fixe : le compteur dénombrait les <i>produits</i> concernés quand la
         * liste rend une ligne par <i>lot</i>. Un produit reçu en trois fois, avec trois dates de
         * péremption, annonçait une seule alerte et en affichait trois.
         */
        @Test
        @DisplayName("trois lots d'un même produit comptent pour trois péremptions")
        void troisLotsDUnMemeProduit() {
            int ecart = ecart(() -> repository.getExpiringProductsCount(30), () -> {
                Produit produit = produitEnStock(unique("TROIS-LOTS"), 30);
                lot(produit, LocalDate.now().plusDays(5), 10);
                lot(produit, LocalDate.now().plusDays(12), 10);
                lot(produit, LocalDate.now().plusDays(20), 10);
            });

            assertThat(ecart).isEqualTo(3);
        }

        @Test
        @DisplayName("le compteur annonce exactement ce que la liste contient")
        void compteurEtListeConcordent() {
            Produit produit = produitEnStock(unique("TROIS-LOTS"), 30);
            lot(produit, LocalDate.now().plusDays(5), 10);
            lot(produit, LocalDate.now().plusDays(12), 10);
            lot(produit, LocalDate.now().plusDays(20), 10);
            em.flush();

            assertThat(repository.getExpiringProductsCount(30)).isEqualTo(repository.getExpiryAlerts(30).size());
        }

        private ExpiryAlertProjection alerteDuProduit(Produit produit) {
            return repository
                .getExpiryAlerts(30)
                .stream()
                .filter(p -> p.productId() == produit.getId())
                .findFirst()
                .orElseThrow();
        }
    }

    // ===== impayés =====

    @Nested
    @DisplayName("Factures impayées")
    class FacturesImpayees {

        @Test
        @DisplayName("une facture ancienne et partiellement réglée est en retard")
        void facturePartiellementReglee() {
            int ecart = ecart(
                () -> repository.getOverdueInvoicesCount(90),
                () -> factureAgee(tiersPayantDeGroupe(), 120, 500_000, 200_000, InvoiceStatut.PARTIALLY_PAID)
            );

            assertThat(ecart).isEqualTo(1);
        }

        @Test
        @DisplayName("une facture soldée n'est pas en retard")
        void factureSoldee() {
            int ecart = ecart(
                () -> repository.getOverdueInvoicesCount(90),
                () -> factureAgee(tiersPayantDeGroupe(), 120, 500_000, 500_000, InvoiceStatut.PARTIALLY_PAID)
            );

            assertThat(ecart).isZero();
        }

        @Test
        @DisplayName("une facture récente n'est pas encore en retard")
        void factureRecente() {
            int ecart = ecart(
                () -> repository.getOverdueInvoicesCount(90),
                () -> factureAgee(tiersPayantDeGroupe(), 30, 500_000, 0, InvoiceStatut.NOT_PAID)
            );

            assertThat(ecart).isZero();
        }

        @Test
        @DisplayName("l'impayé d'un groupe porte le nom et le téléphone du groupe")
        void impayeDeGroupe() {
            GroupeTiersPayant groupe = groupeTiersPayant("CNAM");
            FactureTiersPayant facture = factureAgee(tiersPayant("Caisse nationale", groupe), 120, 500_000, 200_000, InvoiceStatut.PARTIALLY_PAID);
            em.flush();

            OverdueInvoiceProjection impaye = impayeDeLaFacture(facture);

            assertThat(impaye.tiersPayantName()).isEqualTo(groupe.getName());
            assertThat(impaye.telephone()).isEqualTo("0102030405");
            assertThat(impaye.montantFacture()).isEqualTo(500_000L);
            assertThat(impaye.montantRegle()).isEqualTo(200_000L);
            assertThat(impaye.daysOverdue()).isEqualTo(120);
        }

        /**
         * Le défaut que ce test fixe : la liste joignait le groupe de tiers payant en jointure
         * fermée. Une officine dont les tiers payants ne sont pas regroupés — le cas courant —
         * voyait un compteur d'impayés dont la liste restait vide.
         */
        @Test
        @DisplayName("l'impayé d'un tiers payant isolé est listé, non pas seulement compté")
        void impayeSansGroupe() {
            FactureTiersPayant facture = factureAgee(tiersPayant("MUGEF isolé", null), 150, 300_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            OverdueInvoiceProjection impaye = impayeDeLaFacture(facture);

            assertThat(impaye.tiersPayantName()).isEqualTo("MUGEF isolé");
            assertThat(impaye.telephone()).isEqualTo("0607080910");
            assertThat(impaye.montantFacture()).isEqualTo(300_000L);
        }

        @Test
        @DisplayName("le compteur annonce exactement ce que la liste contient")
        void compteurEtListeConcordent() {
            factureAgee(tiersPayantDeGroupe(), 120, 500_000, 200_000, InvoiceStatut.PARTIALLY_PAID);
            factureAgee(tiersPayant("MUGEF isolé", null), 150, 300_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            assertThat(repository.getOverdueInvoicesCount(90)).isEqualTo(repository.getOverdueInvoiceAlerts(90).size());
        }

        private TiersPayant tiersPayantDeGroupe() {
            return tiersPayant("Caisse", groupeTiersPayant("GROUPE"));
        }

        private OverdueInvoiceProjection impayeDeLaFacture(FactureTiersPayant facture) {
            return repository
                .getOverdueInvoiceAlerts(90)
                .stream()
                .filter(p -> p.invoiceId() == facture.getId().getId())
                .findFirst()
                .orElseThrow();
        }
    }

    // ===== écart de caisse =====

    @Nested
    @DisplayName("Écart de caisse")
    class EcartDeCaisse {

        @Test
        @DisplayName("une caisse comptée juste ne produit aucun écart")
        void caisseJuste() {
            LocalDate jour = LocalDate.now().minusDays(1);
            caisseFermee(jour, 500_000L, 500_000L);
            em.flush();

            assertThat(repository.getCashDiscrepancyAmount(jour)).isZero();
        }

        @Test
        @DisplayName("un excédent de caisse est signalé")
        void excedent() {
            LocalDate jour = LocalDate.now().minusDays(2);
            caisseFermee(jour, 500_000L, 507_500L);
            em.flush();

            assertThat(repository.getCashDiscrepancyAmount(jour)).isEqualTo(7_500L);
        }

        /** Un manquant est au moins aussi alarmant qu'un excédent : il compte en valeur absolue. */
        @Test
        @DisplayName("un manquant de caisse est signalé au même titre")
        void manquant() {
            LocalDate jour = LocalDate.now().minusDays(3);
            caisseFermee(jour, 500_000L, 492_500L);
            em.flush();

            assertThat(repository.getCashDiscrepancyAmount(jour)).isEqualTo(7_500L);
        }

        @Test
        @DisplayName("l'écart ne porte que sur la journée demandée")
        void journeeDemandee() {
            caisseFermee(LocalDate.now().minusDays(4), 500_000L, 510_000L);
            em.flush();

            assertThat(repository.getCashDiscrepancyAmount(LocalDate.now().minusDays(5))).isZero();
        }

        @Test
        @DisplayName("une journée sans caisse fermée ne produit aucun écart")
        void aucuneCaisse() {
            assertThat(repository.getCashDiscrepancyAmount(LocalDate.now().minusDays(400))).isZero();
        }
    }

    // ===== cohérence d'ensemble =====

    @Nested
    @DisplayName("Cohérence du bandeau et de la liste")
    class CoherenceDuBandeau {

        /**
         * Le contrat que l'écran attend, toutes natures confondues : le nombre annoncé est le nombre
         * de lignes qu'on obtiendra en ouvrant la liste.
         */
        @Test
        @DisplayName("sur un jeu complet, chaque compteur égale la taille de sa liste")
        void jeuComplet() {
            Produit enRupture = produitEnStock(unique("RUPTURE"), 0);
            Produit aPerimer = produitEnStock(unique("PEREMPTION"), 20);
            lot(aPerimer, LocalDate.now().plusDays(3), 10);
            lot(aPerimer, LocalDate.now().plusDays(25), 10);
            factureAgee(tiersPayant("Caisse", groupeTiersPayant("GROUPE")), 120, 500_000, 100_000, InvoiceStatut.PARTIALLY_PAID);
            factureAgee(tiersPayant("MUGEF isolé", null), 200, 300_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            assertThat(repository.getStockRuptureCount()).isEqualTo(repository.getStockRuptureAlerts().size());
            assertThat(repository.getExpiringProductsCount(30)).isEqualTo(repository.getExpiryAlerts(30).size());
            assertThat(repository.getOverdueInvoicesCount(90)).isEqualTo(repository.getOverdueInvoiceAlerts(90).size());

            List<StockRuptureProjection> ruptures = repository.getStockRuptureAlerts();
            assertThat(ruptures).anyMatch(p -> p.productId() == enRupture.getId());
        }
    }
}
