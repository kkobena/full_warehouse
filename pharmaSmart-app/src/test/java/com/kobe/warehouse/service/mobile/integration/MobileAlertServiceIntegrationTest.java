package com.kobe.warehouse.service.mobile.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.repository.impl.MobileAlertRepositoryImpl;
import com.kobe.warehouse.service.dto.mobile.AlertType;
import com.kobe.warehouse.service.dto.mobile.MobileAlertDetailDTO;
import com.kobe.warehouse.service.mobile.MobileAlertService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * L'écran des alertes se parcourt page par page : il demande une page au service et, séparément, le
 * nombre total d'alertes pour savoir combien de pages afficher. Ce sont deux chemins de code
 * distincts, servis par deux requêtes distinctes.
 *
 * <p>Le contrat est donc simple et impératif : <b>le total annoncé est le nombre de lignes qu'on
 * obtiendra en parcourant toutes les pages</b>. Les tests unitaires du service ne peuvent pas le
 * vérifier — ils lui donnent des chiffres qu'ils ont eux-mêmes fabriqués. Il ne s'éprouve que sur
 * une vraie base, en confrontant les deux requêtes au même jeu de données.
 */
@DisplayName("MobileAlertService — alertes servies depuis PostgreSQL")
class MobileAlertServiceIntegrationTest extends AbstractMobileIntegrationTest {

    private MobileAlertService service;

    @BeforeEach
    void cablerLeService() {
        MobileAlertRepositoryImpl repository = new MobileAlertRepositoryImpl();
        ReflectionTestUtils.setField(repository, "entityManager", em);
        service = new MobileAlertService(repository);
    }

    // ===== accord du compteur et de la liste =====

    @Nested
    @DisplayName("Accord du compteur et de la liste")
    class AccordDuCompteurEtDeLaListe {

        @Test
        @DisplayName("sur un jeu complet, le total annoncé est la taille de la liste")
        void jeuComplet() {
            jeuDAlertes();

            assertThat(service.getAlertsCount(null)).isEqualTo(service.getAlerts(null).size());
        }

        @Test
        @DisplayName("l'accord tient nature par nature")
        void accordParNature() {
            jeuDAlertes();

            for (AlertType type : List.of(AlertType.STOCK_RUPTURE, AlertType.EXPIRY, AlertType.INVOICE_OVERDUE)) {
                assertThat(service.getAlertsCount(List.of(type.getCode())))
                    .as("nature %s", type.getCode())
                    .isEqualTo(service.getAlerts(List.of(type.getCode())).size());
            }
        }

        /** Le cas qui faisait diverger les péremptions : plusieurs lots pour un même produit. */
        @Test
        @DisplayName("plusieurs lots d'un même produit comptent chacun pour une alerte")
        void plusieursLotsDUnProduit() {
            Produit produit = produitEnStock(unique("MULTI-LOTS"), 40);
            lot(produit, LocalDate.now().plusDays(4), 10);
            lot(produit, LocalDate.now().plusDays(11), 10);
            lot(produit, LocalDate.now().plusDays(18), 10);
            em.flush();

            List<String> peremptions = List.of(AlertType.EXPIRY.getCode());
            assertThat(service.getAlerts(peremptions)).hasSize(3);
            assertThat(service.getAlertsCount(peremptions)).isEqualTo(3);
        }

        /** Le cas qui faisait diverger les impayés : un tiers payant sans groupe. */
        @Test
        @DisplayName("la facture d'un tiers payant isolé est comptée et listée")
        void impayeSansGroupe() {
            FactureTiersPayant facture = factureAgee(tiersPayant("MUGEF isolé", null), 150, 300_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            List<String> impayes = List.of(AlertType.INVOICE_OVERDUE.getCode());
            assertThat(service.getAlerts(impayes))
                .extracting(MobileAlertDetailDTO::relatedEntityId)
                .contains(facture.getId().getId());
            assertThat(service.getAlertsCount(impayes)).isEqualTo(service.getAlerts(impayes).size());
        }
    }

    // ===== pagination =====

    @Nested
    @DisplayName("Pagination")
    class Pagination {

        @Test
        @DisplayName("parcourir toutes les pages rend exactement le total annoncé")
        void parcoursComplet() {
            jeuDAlertes();
            long total = service.getAlertsCount(null);
            int taille = 2;

            int lues = 0;
            for (int page = 0; lues < total; page++) {
                int surLaPage = service.getAlerts(null, page, taille).size();
                assertThat(surLaPage).as("page %d", page).isPositive();
                lues += surLaPage;
            }

            assertThat(lues).isEqualTo((int) total);
        }

        @Test
        @DisplayName("la page qui suit la dernière est vide")
        void pageApresLaFin() {
            jeuDAlertes();
            long total = service.getAlertsCount(null);

            assertThat(service.getAlerts(null, (int) total + 1, 1)).isEmpty();
        }
    }

    // ===== bandeau =====

    @Nested
    @DisplayName("Bandeau de l'écran d'accueil")
    class BandeauDeLEcranDAccueil {

        @Test
        @DisplayName("chaque nature présente forme une ligne du bandeau")
        void naturesPresentes() {
            jeuDAlertes();

            assertThat(service.getAlertsSummary())
                .extracting(com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO.MobileAlertDTO::type)
                .contains(AlertType.STOCK_RUPTURE.getCode(), AlertType.EXPIRY.getCode(), AlertType.INVOICE_OVERDUE.getCode());
        }

        /**
         * Le bandeau annonce les mêmes nombres que la liste détaillée — à l'écart de caisse près, qui
         * n'a pas de détail à afficher et ne compte donc pas parmi les alertes parcourables.
         */
        @Test
        @DisplayName("les nombres du bandeau sont ceux de la liste détaillée")
        void nombresDuBandeau() {
            jeuDAlertes();

            int totalDuBandeau = service
                .getAlertsSummary()
                .stream()
                .filter(a -> !AlertType.CASH_DISCREPANCY.getCode().equals(a.type()))
                .mapToInt(com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO.MobileAlertDTO::count)
                .sum();

            assertThat(totalDuBandeau).isEqualTo(service.getAlerts(null).size());
        }
    }

    // ===== jeu d'essai =====

    /** Une officine ordinaire : des ruptures, des lots qui approchent, des factures qui traînent. */
    private void jeuDAlertes() {
        produitEnStock(unique("RUPTURE"), 0);
        produitEnStock(unique("RUPTURE"), 0);
        produitEnStock(unique("DISPO"), 40);

        Produit aPerimer = produitEnStock(unique("PEREMPTION"), 40);
        lot(aPerimer, LocalDate.now().plusDays(6), 10);
        lot(aPerimer, LocalDate.now().plusDays(22), 10);

        factureAgee(tiersPayant("Caisse", groupeTiersPayant("CNAM")), 120, 500_000, 100_000, InvoiceStatut.PARTIALLY_PAID);
        factureAgee(tiersPayant("MUGEF isolé", null), 200, 300_000, 0, InvoiceStatut.NOT_PAID);
        em.flush();
    }
}
