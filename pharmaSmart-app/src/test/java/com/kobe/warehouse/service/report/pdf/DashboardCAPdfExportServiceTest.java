package com.kobe.warehouse.service.report.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.report.DashboardCASummaryDTO;
import com.kobe.warehouse.service.dto.report.PaymentMethodCADTO;
import com.kobe.warehouse.service.dto.report.ProductFamilyCADTO;
import com.kobe.warehouse.service.dto.report.TopProductDTO;
import com.kobe.warehouse.service.report.DashboardCAService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Le rapport comparatif des ventes est le seul document PDF qui <b>recalcule</b> quelque chose au
 * lieu de reprendre ce qu'un service lui donne : il regroupe les modes de paiement et les familles
 * de produits, qui lui arrivent ventilés jour par jour, et en refait les totaux sur la période.
 *
 * <p>Deux agrégations s'y jouent, et la seconde est piégeuse. Les <b>montants</b> s'additionnent —
 * trois journées de paiements en espèces font un total d'espèces. Mais le <b>taux de marge</b> ne
 * s'additionne pas : additionner trois taux de 30 % donnerait 90 %. Il doit être recalculé sur les
 * totaux agrégés, et c'est exactement ce que ces tests tiennent.
 *
 * <p>Le rendu PDF lui-même est court-circuité — le moteur de gabarits est simulé. Ce qui se vérifie
 * ici est le <b>modèle</b> remis au gabarit : un chiffre faux dans le modèle donne un PDF
 * impeccablement mis en page et faux.
 */
@DisplayName("DashboardCAPdfExportService — agrégation du rapport comparatif")
class DashboardCAPdfExportServiceTest {

    private DashboardCAService dashboardCAService;
    private DashboardCAPdfExportService service;

    @BeforeEach
    void setUp() {
        dashboardCAService = mock(DashboardCAService.class);

        SpringTemplateEngine moteurDeGabarits = mock(SpringTemplateEngine.class);
        when(moteurDeGabarits.process(anyString(), any(Context.class))).thenReturn("<html><body>rapport</body></html>");

        Magasin magasin = new Magasin();
        magasin.setRegistre("RC-1");
        AppUser utilisateur = new AppUser();
        utilisateur.setMagasin(magasin);
        StorageService storageService = mock(StorageService.class);
        when(storageService.getUser()).thenReturn(utilisateur);

        service = new DashboardCAPdfExportService(storageService, moteurDeGabarits, dashboardCAService);

        when(dashboardCAService.getOverallSummary()).thenReturn(resumeVide());
        when(dashboardCAService.getPaymentMethodDistribution(any(), any())).thenReturn(List.of());
        when(dashboardCAService.getProductFamilyDistribution(any(), any())).thenReturn(List.of());
        when(dashboardCAService.getTopProducts(any(), any(), any())).thenReturn(List.of());
    }

    // ===== modes de paiement =====

    @Nested
    @DisplayName("Modes de paiement")
    class ModesDePaiement {

        /** Les données arrivent ventilées par jour : trois journées d'espèces font une seule ligne. */
        @Test
        @DisplayName("les journées d'un même mode sont regroupées en une ligne")
        void regroupementParMode() {
            when(dashboardCAService.getPaymentMethodDistribution(any(), any())).thenReturn(
                List.of(paiement("ESPECE", "CASH", 100_000L, 10), paiement("ESPECE", "CASH", 50_000L, 5), paiement("WAVE", "WAVE", 30_000L, 3))
            );

            Map<String, Object> modele = exporter();

            assertThat(lignesDePaiement(modele)).hasSize(2);
            assertThat(lignesDePaiement(modele).getFirst().toString()).contains("ESPECE");
        }

        @Test
        @DisplayName("les montants et le nombre de règlements s'additionnent")
        void additionDesMontants() {
            when(dashboardCAService.getPaymentMethodDistribution(any(), any())).thenReturn(
                List.of(paiement("ESPECE", "CASH", 100_000L, 10), paiement("ESPECE", "CASH", 50_000L, 5))
            );

            Object ligne = lignesDePaiement(exporter()).getFirst();

            assertThat(ligne.toString()).contains("150000").contains("15");
        }

        /** Les modes les plus gros en tête : c'est l'ordre dans lequel on lit un état de caisse. */
        @Test
        @DisplayName("les modes les plus utilisés passent en tête")
        void ordreParMontant() {
            when(dashboardCAService.getPaymentMethodDistribution(any(), any())).thenReturn(
                List.of(paiement("WAVE", "WAVE", 30_000L, 3), paiement("ESPECE", "CASH", 150_000L, 15))
            );

            assertThat(lignesDePaiement(exporter()).getFirst().toString()).contains("ESPECE");
        }
    }

    // ===== familles de produits =====

    @Nested
    @DisplayName("Familles de produits")
    class FamillesDeProduits {

        @Test
        @DisplayName("les journées d'une même famille sont regroupées")
        void regroupementParFamille() {
            when(dashboardCAService.getProductFamilyDistribution(any(), any())).thenReturn(
                List.of(famille("MEDICAMENT", 100_000L, 30_000L, "30.00"), famille("MEDICAMENT", 300_000L, 90_000L, "30.00"))
            );

            Map<String, Object> modele = exporter();

            assertThat(lignesDeFamille(modele)).hasSize(1);
            assertThat(modele.get("totalFamilyCA")).isEqualTo(400_000L);
            assertThat(modele.get("totalFamilyMarge")).isEqualTo(120_000L);
        }

        /**
         * Le piège : additionner trois taux de 30 % donnerait 90 %. Le taux est donc recalculé sur
         * les totaux — marge cumulée rapportée au chiffre d'affaires cumulé.
         */
        @Test
        @DisplayName("le taux de marge est recalculé, jamais additionné")
        void tauxDeMargeRecalcule() {
            when(dashboardCAService.getProductFamilyDistribution(any(), any())).thenReturn(
                List.of(famille("MEDICAMENT", 100_000L, 30_000L, "30.00"), famille("MEDICAMENT", 300_000L, 90_000L, "30.00"))
            );

            assertThat(lignesDeFamille(exporter()).getFirst().toString()).contains("30.00").doesNotContain("60.00");
        }

        @Test
        @DisplayName("des taux différents se combinent en un taux pondéré")
        void tauxPondere() {
            when(dashboardCAService.getProductFamilyDistribution(any(), any())).thenReturn(
                // 200 000 à 10 % et 200 000 à 50 % → 120 000 de marge sur 400 000, soit 30 %
                List.of(famille("PARAPHARMACIE", 200_000L, 20_000L, "10.00"), famille("PARAPHARMACIE", 200_000L, 100_000L, "50.00"))
            );

            assertThat(lignesDeFamille(exporter()).getFirst().toString()).contains("30.00");
        }

        /** Une famille sans chiffre d'affaires ne se divise pas : son taux reste à zéro. */
        @Test
        @DisplayName("une famille sans chiffre d'affaires garde un taux nul")
        void familleSansChiffreDAffaires() {
            when(dashboardCAService.getProductFamilyDistribution(any(), any())).thenReturn(
                List.of(famille("DORMANTE", 0L, 0L, "0.00"))
            );

            Map<String, Object> modele = exporter();

            assertThat(lignesDeFamille(modele)).hasSize(1);
            assertThat(modele.get("totalFamilyCA")).isEqualTo(0L);
        }

        @Test
        @DisplayName("les familles les plus vendeuses passent en tête")
        void ordreParChiffreDAffaires() {
            when(dashboardCAService.getProductFamilyDistribution(any(), any())).thenReturn(
                List.of(famille("PETITE", 10_000L, 1_000L, "10.00"), famille("GROSSE", 900_000L, 90_000L, "10.00"))
            );

            assertThat(lignesDeFamille(exporter()).getFirst().toString()).contains("GROSSE");
        }
    }

    // ===== modèle remis au gabarit =====

    @Nested
    @DisplayName("Modèle remis au gabarit")
    class Modele {

        @Test
        @DisplayName("le palmarès des produits et son total accompagnent le document")
        void palmaresEtTotal() {
            when(dashboardCAService.getTopProducts(any(), any(), any())).thenReturn(
                List.of(topProduit("DOLIPRANE", 300_000), topProduit("EFFERALGAN", 200_000))
            );

            Map<String, Object> modele = exporter();

            assertThat(modele.get("totalTopProductsCA")).isEqualTo(500_000L);
            assertThat((List<?>) modele.get("topProducts")).hasSize(2);
        }

        @Test
        @DisplayName("la période demandée est reportée dans le modèle")
        void periodeReportee() {
            Map<String, Object> modele = exporter();

            assertThat(modele.get("startDate")).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(modele.get("endDate")).isEqualTo(LocalDate.of(2026, 3, 31));
        }

        @Test
        @DisplayName("le magasin et son pied de page légal sont joints au document")
        void mentionsLegales() {
            Map<String, Object> modele = exporter();

            assertThat(modele).containsKey("magasin");
            assertThat(modele.get("footer").toString()).contains("RC N° RC-1");
        }

        @Test
        @DisplayName("sans aucune donnée, le document se produit avec des totaux à zéro")
        void aucuneDonnee() {
            Map<String, Object> modele = exporter();

            assertThat(modele.get("totalFamilyCA")).isEqualTo(0L);
            assertThat(modele.get("totalFamilyMarge")).isEqualTo(0L);
            assertThat(modele.get("totalTopProductsCA")).isEqualTo(0L);
            assertThat(lignesDePaiement(modele)).isEmpty();
            assertThat(lignesDeFamille(modele)).isEmpty();
        }
    }

    // ===== fabriques =====

    private Map<String, Object> exporter() {
        service.export(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
        return service.getParameters();
    }

    private static List<?> lignesDePaiement(Map<String, Object> modele) {
        return (List<?>) modele.get("paymentMethods");
    }

    private static List<?> lignesDeFamille(Map<String, Object> modele) {
        return (List<?>) modele.get("productFamilies");
    }

    private static PaymentMethodCADTO paiement(String libelle, String code, long montant, int nombre) {
        return new PaymentMethodCADTO(LocalDate.of(2026, 1, 15), libelle, code, nombre, montant, BigDecimal.ZERO);
    }

    private static ProductFamilyCADTO famille(String nom, long caTotal, long margeBrute, String tauxMarge) {
        return new ProductFamilyCADTO(LocalDate.of(2026, 1, 15), nom, 1, caTotal, caTotal - margeBrute, margeBrute, new BigDecimal(tauxMarge), 1);
    }

    private static TopProductDTO topProduit(String libelle, int caGenere) {
        return new TopProductDTO(LocalDate.of(2026, 1, 1), 1, libelle, "CIP", 1L, 1, caGenere, BigDecimal.ZERO);
    }

    private static DashboardCASummaryDTO resumeVide() {
        return new DashboardCASummaryDTO(
            0L, 0L, BigDecimal.ZERO,
            0L, 0L, BigDecimal.ZERO,
            0L, 0L, BigDecimal.ZERO,
            0L, 0L, BigDecimal.ZERO,
            0, 0, 0, 0,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
    }
}
