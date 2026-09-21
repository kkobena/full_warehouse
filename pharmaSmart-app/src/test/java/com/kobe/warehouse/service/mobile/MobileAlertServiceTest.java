package com.kobe.warehouse.service.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.repository.MobileAlertRepository;
import com.kobe.warehouse.repository.MobileAlertRepository.ExpiryAlertProjection;
import com.kobe.warehouse.repository.MobileAlertRepository.OverdueInvoiceProjection;
import com.kobe.warehouse.repository.MobileAlertRepository.StockRuptureProjection;
import com.kobe.warehouse.service.dto.mobile.AlertSeverity;
import com.kobe.warehouse.service.dto.mobile.AlertType;
import com.kobe.warehouse.service.dto.mobile.MobileAlertDetailDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO.MobileAlertDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les alertes sont ce que le pharmacien voit en premier en ouvrant l'application. Elles se
 * présentent sous deux formes : un bandeau de compteurs, et une liste détaillée paginée.
 *
 * <p>Deux règles de conduite s'y jouent. La première est le <b>silence quand tout va bien</b> : un
 * compteur à zéro ne doit pas produire de ligne, faute de quoi le bandeau afficherait en permanence
 * « 0 ruptures de stock » et cesserait d'être lu. La seconde est la <b>gradation</b> : la gravité
 * n'est pas attachée au type d'alerte mais au dépassement d'un seuil, et c'est la couleur qui en
 * découle qui fait qu'un lot périmant dans trois jours se distingue d'un lot périmant dans un mois.
 */
@DisplayName("MobileAlertService — alertes de l'écran d'accueil")
class MobileAlertServiceTest {

    // Doublure créée à l'initialisation du champ, et non dans un @BeforeEach : le délai de quinze
    // secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final MobileAlertRepository alertRepository = mock(MobileAlertRepository.class);

    private final MobileAlertService service = new MobileAlertService(alertRepository);

    @BeforeEach
    void toutVaBien() {
        when(alertRepository.getStockRuptureCount()).thenReturn(0);
        when(alertRepository.getExpiringProductsCount(anyInt())).thenReturn(0);
        when(alertRepository.getCashDiscrepancyAmount(any())).thenReturn(0L);
        when(alertRepository.getOverdueInvoicesCount(anyInt())).thenReturn(0);
        when(alertRepository.getStockRuptureAlerts()).thenReturn(List.of());
        when(alertRepository.getExpiryAlerts(anyInt())).thenReturn(List.of());
        when(alertRepository.getOverdueInvoiceAlerts(anyInt())).thenReturn(List.of());
    }

    // ===== bandeau de compteurs =====

    @Nested
    @DisplayName("Bandeau de compteurs")
    class BandeauDeCompteurs {

        @Test
        @DisplayName("une officine sans anomalie n'affiche aucune alerte")
        void aucuneAnomalie() {
            assertThat(service.getAlertsSummary()).isEmpty();
        }

        @Test
        @DisplayName("les ruptures de stock sont critiques")
        void rupturesCritiques() {
            when(alertRepository.getStockRuptureCount()).thenReturn(7);

            MobileAlertDTO alerte = service.getAlertsSummary().getFirst();

            assertThat(alerte.type()).isEqualTo(AlertType.STOCK_RUPTURE.getCode());
            assertThat(alerte.severity()).isEqualTo(AlertSeverity.CRITICAL.getCode());
            assertThat(alerte.count()).isEqualTo(7);
            assertThat(alerte.message()).isEqualTo("7 ruptures de stock");
            assertThat(alerte.icon()).isEqualTo(AlertType.STOCK_RUPTURE.getIcon());
            assertThat(alerte.color()).isEqualTo(AlertSeverity.CRITICAL.getColor());
        }

        @Test
        @DisplayName("les péremptions sont guettées sur trente jours")
        void peremptionsTrenteJours() {
            when(alertRepository.getExpiringProductsCount(30)).thenReturn(4);

            MobileAlertDTO alerte = service.getAlertsSummary().getFirst();

            assertThat(alerte.type()).isEqualTo(AlertType.EXPIRY.getCode());
            assertThat(alerte.severity()).isEqualTo(AlertSeverity.WARNING.getCode());
            assertThat(alerte.message()).isEqualTo("4 peremptions < 30j");
        }

        @Test
        @DisplayName("les impayés sont guettés au-delà de quatre-vingt-dix jours")
        void impayesQuatreVingtDixJours() {
            when(alertRepository.getOverdueInvoicesCount(90)).thenReturn(3);

            MobileAlertDTO alerte = service.getAlertsSummary().getFirst();

            assertThat(alerte.type()).isEqualTo(AlertType.INVOICE_OVERDUE.getCode());
            assertThat(alerte.severity()).isEqualTo(AlertSeverity.INFO.getCode());
            assertThat(alerte.message()).isEqualTo("3 factures impayees > 90j");
        }

        /**
         * L'écart de caisse compte pour une seule ligne quel que soit le nombre de caisses : c'est le
         * montant qui alerte, pas le nombre.
         */
        @Test
        @DisplayName("l'écart de caisse du jour compte pour une ligne, montant en clair")
        void ecartDeCaisse() {
            when(alertRepository.getCashDiscrepancyAmount(LocalDate.now())).thenReturn(3_500L);

            MobileAlertDTO alerte = service.getAlertsSummary().getFirst();

            assertThat(alerte.type()).isEqualTo(AlertType.CASH_DISCREPANCY.getCode());
            assertThat(alerte.count()).isEqualTo(1);
            assertThat(alerte.message()).containsPattern("Ecart caisse: 3.500 F");
        }

        @Test
        @DisplayName("un écart de caisse au-delà de dix mille francs devient critique")
        void ecartDeCaisseCritique() {
            when(alertRepository.getCashDiscrepancyAmount(any())).thenReturn(10_001L);

            assertThat(service.getAlertsSummary().getFirst().severity()).isEqualTo(AlertSeverity.CRITICAL.getCode());
        }

        @Test
        @DisplayName("un écart de caisse en deçà du seuil reste un avertissement")
        void ecartDeCaisseModere() {
            when(alertRepository.getCashDiscrepancyAmount(any())).thenReturn(10_000L);

            assertThat(service.getAlertsSummary().getFirst().severity()).isEqualTo(AlertSeverity.WARNING.getCode());
        }

        @Test
        @DisplayName("les alertes se présentent du plus critique au plus informatif")
        void ordreDeGravite() {
            when(alertRepository.getStockRuptureCount()).thenReturn(1);
            when(alertRepository.getExpiringProductsCount(30)).thenReturn(1);
            when(alertRepository.getCashDiscrepancyAmount(any())).thenReturn(500L);
            when(alertRepository.getOverdueInvoicesCount(90)).thenReturn(1);

            assertThat(service.getAlertsSummary())
                .extracting(MobileAlertDTO::type)
                .containsExactly("STOCK_RUPTURE", "EXPIRY", "CASH_DISCREPANCY", "INVOICE_OVERDUE");
        }
    }

    // ===== liste détaillée =====

    @Nested
    @DisplayName("Liste détaillée")
    class ListeDetaillee {

        @Test
        @DisplayName("une rupture désigne le produit et propose de l'ouvrir")
        void ruptureDeStock() {
            when(alertRepository.getStockRuptureAlerts()).thenReturn(List.of(rupture(42L, "DOLIPRANE 1000")));

            MobileAlertDetailDTO alerte = service.getAlerts(null).getFirst();

            assertThat(alerte.id()).isEqualTo(42L);
            assertThat(alerte.severity()).isEqualTo(AlertSeverity.CRITICAL.getCode());
            assertThat(alerte.title()).isEqualTo(AlertType.STOCK_RUPTURE.getLibelle());
            assertThat(alerte.message()).isEqualTo("DOLIPRANE 1000 - Stock epuise");
            assertThat(alerte.actionType()).isEqualTo("VIEW_PRODUCT");
            assertThat(alerte.actionData()).containsEntry("productId", 42L);
            assertThat(alerte.relatedEntityType()).isEqualTo("PRODUCT");
            assertThat(alerte.relatedEntityName()).isEqualTo("DOLIPRANE 1000");
        }

        @Test
        @DisplayName("une péremption annonce le délai restant et désigne le lot")
        void peremptionDetaillee() {
            when(alertRepository.getExpiryAlerts(30)).thenReturn(List.of(peremption(9L, 42L, "EFFERALGAN", 12)));

            MobileAlertDetailDTO alerte = service.getAlerts(null).getFirst();

            assertThat(alerte.id()).isEqualTo(9L);
            assertThat(alerte.message()).isEqualTo("EFFERALGAN - Expire dans 12 jours");
            assertThat(alerte.actionData()).containsEntry("productId", 42L).containsEntry("lotId", 9L);
            assertThat(alerte.relatedEntityId()).isEqualTo(42L);
        }

        /** Une semaine : au-delà, le lot se réserve ou se retourne ; en deçà, il se perd. */
        @Test
        @DisplayName("une péremption à moins d'une semaine devient critique")
        void peremptionImminente() {
            when(alertRepository.getExpiryAlerts(30)).thenReturn(
                List.of(peremption(1L, 1L, "A", 7), peremption(2L, 2L, "B", 8))
            );

            assertThat(service.getAlerts(null))
                .extracting(MobileAlertDetailDTO::severity)
                .containsExactly(AlertSeverity.CRITICAL.getCode(), AlertSeverity.WARNING.getCode());
        }

        @Test
        @DisplayName("un impayé annonce le reste dû et propose d'appeler le payeur")
        void impayeDetaille() {
            when(alertRepository.getOverdueInvoiceAlerts(90)).thenReturn(
                List.of(impaye(77L, "CNAM", "0102030405", 500_000L, 200_000L, 120))
            );

            MobileAlertDetailDTO alerte = service.getAlerts(null).getFirst();

            assertThat(alerte.id()).isEqualTo(77L);
            assertThat(alerte.message()).containsPattern("CNAM - 300.000 F depuis 120j");
            assertThat(alerte.actionType()).isEqualTo("CALL_CLIENT");
            assertThat(alerte.actionData()).containsEntry("invoiceId", 77L).containsEntry("phone", "0102030405");
            assertThat(alerte.relatedEntityType()).isEqualTo("INVOICE");
        }

        /** Un payeur sans téléphone ne doit pas faire échouer l'écran : l'action part vide. */
        @Test
        @DisplayName("un payeur sans téléphone laisse l'action sans numéro")
        void impayeSansTelephone() {
            when(alertRepository.getOverdueInvoiceAlerts(90)).thenReturn(
                List.of(impaye(77L, "CNAM", null, 500_000L, 0L, 100))
            );

            assertThat(service.getAlerts(null).getFirst().actionData()).containsEntry("phone", "");
        }

        @Test
        @DisplayName("un impayé de plus de six mois devient critique")
        void impayeTresAncien() {
            when(alertRepository.getOverdueInvoiceAlerts(90)).thenReturn(
                List.of(impaye(1L, "A", null, 1L, 0L, 181), impaye(2L, "B", null, 1L, 0L, 180))
            );

            assertThat(service.getAlerts(null))
                .extracting(MobileAlertDetailDTO::severity)
                .containsExactly(AlertSeverity.CRITICAL.getCode(), AlertSeverity.WARNING.getCode());
        }
    }

    // ===== filtrage par type =====

    @Nested
    @DisplayName("Filtrage par type")
    class FiltrageParType {

        @Test
        @DisplayName("un type demandé n'interroge que la source correspondante")
        void unSeulType() {
            service.getAlerts(List.of(AlertType.EXPIRY.getCode()));

            verify(alertRepository).getExpiryAlerts(30);
            verify(alertRepository, never()).getStockRuptureAlerts();
            verify(alertRepository, never()).getOverdueInvoiceAlerts(anyInt());
        }

        @Test
        @DisplayName("aucun type demandé rassemble toutes les alertes")
        void tousLesTypes() {
            service.getAlerts(null);

            verify(alertRepository).getStockRuptureAlerts();
            verify(alertRepository).getExpiryAlerts(30);
            verify(alertRepository).getOverdueInvoiceAlerts(90);
        }

        @Test
        @DisplayName("une liste de types vide vaut « tous les types »")
        void listeVide() {
            service.getAlerts(List.of());

            verify(alertRepository).getStockRuptureAlerts();
            verify(alertRepository).getExpiryAlerts(30);
            verify(alertRepository).getOverdueInvoiceAlerts(90);
        }

        @Test
        @DisplayName("un type inconnu ne ramène rien plutôt que tout")
        void typeInconnu() {
            assertThat(service.getAlerts(List.of("TYPE_DISPARU"))).isEmpty();
        }

        @Test
        @DisplayName("le décompte suit le même filtrage que la liste")
        void decompteFiltre() {
            when(alertRepository.getStockRuptureCount()).thenReturn(5);
            when(alertRepository.getExpiringProductsCount(30)).thenReturn(3);
            when(alertRepository.getOverdueInvoicesCount(90)).thenReturn(2);

            assertThat(service.getAlertsCount(null)).isEqualTo(10);
            assertThat(service.getAlertsCount(List.of(AlertType.STOCK_RUPTURE.getCode()))).isEqualTo(5);
            assertThat(service.getAlertsCount(List.of("TYPE_DISPARU"))).isZero();
        }
    }

    // ===== pagination =====

    @Nested
    @DisplayName("Pagination")
    class Pagination {

        @BeforeEach
        void septRuptures() {
            when(alertRepository.getStockRuptureAlerts()).thenReturn(
                List.of(
                    rupture(1L, "P1"), rupture(2L, "P2"), rupture(3L, "P3"), rupture(4L, "P4"),
                    rupture(5L, "P5"), rupture(6L, "P6"), rupture(7L, "P7")
                )
            );
        }

        @Test
        @DisplayName("la première page rend le début de la liste")
        void premierePage() {
            assertThat(service.getAlerts(null, 0, 3))
                .extracting(MobileAlertDetailDTO::relatedEntityName)
                .containsExactly("P1", "P2", "P3");
        }

        @Test
        @DisplayName("la dernière page rend ce qui reste, sans le compléter")
        void dernierePage() {
            assertThat(service.getAlerts(null, 2, 3))
                .extracting(MobileAlertDetailDTO::relatedEntityName)
                .containsExactly("P7");
        }

        /** Le téléphone peut redemander une page au-delà de la fin : elle doit être vide, pas fatale. */
        @Test
        @DisplayName("une page au-delà de la fin est vide")
        void pageAuDela() {
            assertThat(service.getAlerts(null, 9, 3)).isEmpty();
        }

        @Test
        @DisplayName("sans pagination demandée, toutes les alertes sont rendues")
        void sansPagination() {
            assertThat(service.getAlerts(null)).hasSize(7);
        }
    }

    // ===== fabriques =====

    private static StockRuptureProjection rupture(long produitId, String libelle) {
        return new StockRuptureProjection(produitId, libelle, "CIP" + produitId, 0, 0);
    }

    private static ExpiryAlertProjection peremption(long lotId, long produitId, String libelle, int joursRestants) {
        return new ExpiryAlertProjection(
            lotId,
            produitId,
            libelle,
            "LOT" + lotId,
            LocalDate.now().plusDays(joursRestants),
            10,
            joursRestants
        );
    }

    private static OverdueInvoiceProjection impaye(
        long factureId,
        String payeur,
        String telephone,
        long montantFacture,
        long montantRegle,
        int joursDeRetard
    ) {
        return new OverdueInvoiceProjection(
            factureId,
            LocalDate.now().minusDays(joursDeRetard),
            payeur,
            telephone,
            montantFacture,
            montantRegle,
            joursDeRetard
        );
    }
}
