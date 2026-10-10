package com.kobe.warehouse.service.report.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.CategorieABC;
import com.kobe.warehouse.domain.enumeration.StockAlertType;
import com.kobe.warehouse.service.dto.report.CustomerSegmentationDTO;
import com.kobe.warehouse.service.dto.report.CustomerSegmentationDTO.CustomerClassification;
import com.kobe.warehouse.service.dto.report.StockAlertDTO;
import com.kobe.warehouse.service.dto.report.StockRotationDTO;
import com.kobe.warehouse.service.report.CustomerSegmentationReportService;
import com.kobe.warehouse.service.report.StockAlertReportService;
import com.kobe.warehouse.service.report.StockRotationReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Trois documents affichent en tête un bandeau de compteurs — tant de ruptures, tant de champions,
 * tant de produits à forte rotation — avant de dérouler le détail. Ces compteurs viennent d'une
 * table indexée par classe, et chacun est extrait nommément.
 *
 * <p>Deux défauts guettent une telle extraction, et aucun ne se voit sur le document produit. Une
 * <b>classe oubliée</b> : le bandeau affiche alors un trou, ou pire, la valeur d'une autre classe.
 * Et une <b>classe absente de la table</b> — cas normal quand aucun produit n'est concerné — qui
 * doit se lire « 0 » et non disparaître, sans quoi le lecteur ne sait pas si le compteur vaut zéro
 * ou si le rapport a échoué.
 */
@DisplayName("Générateurs PDF à bandeau de compteurs")
class CompteursParClassePdfReportServicesTest {

    // ===== alertes de stock =====

    @Nested
    @DisplayName("Alertes de stock")
    class AlertesDeStock {

        private final StockAlertReportService reportService = mock(StockAlertReportService.class);
        private final StockAlertPdfReportService service = new StockAlertPdfReportService(
            PdfReportTestSupport.storageService(),
            PdfReportTestSupport.moteurDeGabarits(),
            reportService
        );

        @Test
        @DisplayName("chaque type d'alerte a son compteur")
        void compteursParType() {
            when(reportService.getStockAlerts(any(), any(Pageable.class))).thenReturn(alertes());
            when(reportService.getStockAlertsCount()).thenReturn(
                Map.of(StockAlertType.RUPTURE, 12L, StockAlertType.ALERTE, 34L, StockAlertType.PEREMPTION, 5L)
            );

            Map<String, Object> modele = modele();

            assertThat(modele.get("ruptureCount")).isEqualTo(12L);
            assertThat(modele.get("alerteCount")).isEqualTo(34L);
            assertThat(modele.get("peremptionCount")).isEqualTo(5L);
        }

        /** Aucune péremption en cours est une bonne nouvelle, pas un compteur manquant. */
        @Test
        @DisplayName("un type absent de la table se lit zéro")
        void typeAbsent() {
            when(reportService.getStockAlerts(any(), any(Pageable.class))).thenReturn(alertes());
            when(reportService.getStockAlertsCount()).thenReturn(Map.of(StockAlertType.RUPTURE, 12L));

            Map<String, Object> modele = modele();

            assertThat(modele.get("ruptureCount")).isEqualTo(12L);
            assertThat(modele.get("alerteCount")).isEqualTo(0L);
            assertThat(modele.get("peremptionCount")).isEqualTo(0L);
        }

        @Test
        @DisplayName("le détail des alertes et les mentions légales accompagnent le document")
        void modeleDuDocument() {
            when(reportService.getStockAlerts(any(), any(Pageable.class))).thenReturn(alertes());
            when(reportService.getStockAlertsCount()).thenReturn(Map.of());

            Map<String, Object> modele = modele();

            assertThat((List<?>) modele.get("alerts")).hasSize(2);
            assertThat(modele.get("reportTitle")).isEqualTo("Rapport d'Alertes Stock");
            assertThat(modele.get("footer").toString()).contains("RC N° " + PdfReportTestSupport.REGISTRE);
        }

        private Map<String, Object> modele() {
            service.export(List.of(StockAlertType.RUPTURE));
            return service.getParameters();
        }

        private Page<StockAlertDTO> alertes() {
            return new PageImpl<>(
                List.of(
                    new StockAlertDTO(1, "DOLIPRANE", "CIP1", 0, 5, LocalDate.now(), StockAlertType.RUPTURE),
                    new StockAlertDTO(2, "EFFERALGAN", "CIP2", 3, 10, LocalDate.now(), StockAlertType.ALERTE)
                )
            );
        }
    }

    // ===== segmentation client =====

    @Nested
    @DisplayName("Segmentation client")
    class SegmentationClient {

        private final CustomerSegmentationReportService reportService = mock(CustomerSegmentationReportService.class);
        private final CustomerSegmentationPdfReportService service = new CustomerSegmentationPdfReportService(
            PdfReportTestSupport.storageService(),
            PdfReportTestSupport.moteurDeGabarits(),
            reportService
        );

        /** Les sept classes RFM ont chacune leur compteur : en oublier une laisserait un trou au bandeau. */
        @Test
        @DisplayName("les sept classes RFM ont chacune leur compteur")
        void septCompteurs() {
            when(reportService.getAllCustomerSegmentation()).thenReturn(List.of(client()));
            when(reportService.getCustomerCountByClassification()).thenReturn(
                Map.of(
                    CustomerClassification.CHAMPION, 1L,
                    CustomerClassification.LOYAL, 2L,
                    CustomerClassification.BIG_SPENDER, 3L,
                    CustomerClassification.ACTIVE, 4L,
                    CustomerClassification.AT_RISK, 5L,
                    CustomerClassification.NEED_ATTENTION, 6L,
                    CustomerClassification.INACTIVE, 7L
                )
            );

            Map<String, Object> modele = modele();

            assertThat(modele.get("championCount")).isEqualTo(1L);
            assertThat(modele.get("loyalCount")).isEqualTo(2L);
            assertThat(modele.get("bigSpenderCount")).isEqualTo(3L);
            assertThat(modele.get("activeCount")).isEqualTo(4L);
            assertThat(modele.get("atRiskCount")).isEqualTo(5L);
            assertThat(modele.get("needAttentionCount")).isEqualTo(6L);
            assertThat(modele.get("inactiveCount")).isEqualTo(7L);
        }

        @Test
        @DisplayName("une classe sans client se lit zéro")
        void classeAbsente() {
            when(reportService.getAllCustomerSegmentation()).thenReturn(List.of());
            when(reportService.getCustomerCountByClassification()).thenReturn(Map.of(CustomerClassification.CHAMPION, 3L));

            Map<String, Object> modele = modele();

            assertThat(modele.get("championCount")).isEqualTo(3L);
            assertThat(modele.get("inactiveCount")).isEqualTo(0L);
            assertThat(modele.get("atRiskCount")).isEqualTo(0L);
        }

        @Test
        @DisplayName("le détail des clients et les mentions légales accompagnent le document")
        void modeleDuDocument() {
            when(reportService.getAllCustomerSegmentation()).thenReturn(List.of(client(), client()));
            when(reportService.getCustomerCountByClassification()).thenReturn(Map.of());

            Map<String, Object> modele = modele();

            assertThat((List<?>) modele.get("segmentations")).hasSize(2);
            assertThat(modele.get("reportTitle")).isEqualTo("Rapport de Segmentation Client (RFM)");
            assertThat(modele.get("footer").toString()).contains("RC N° " + PdfReportTestSupport.REGISTRE);
        }

        private Map<String, Object> modele() {
            service.export();
            return service.getParameters();
        }

        private CustomerSegmentationDTO client() {
            return new CustomerSegmentationDTO(
                1, "Jean KOUASSI", "0102030405", LocalDate.now(), 5, 10, 500_000, BigDecimal.TEN, 5, 4, 5, 545,
                CustomerClassification.CHAMPION
            );
        }
    }

    // ===== rotation du stock =====

    @Nested
    @DisplayName("Rotation du stock")
    class RotationDuStock {

        private final StockRotationReportService reportService = mock(StockRotationReportService.class);
        private final StockRotationPdfReportService service = new StockRotationPdfReportService(
            PdfReportTestSupport.storageService(),
            PdfReportTestSupport.moteurDeGabarits(),
            reportService
        );

        @Test
        @DisplayName("chaque catégorie de rotation a son compteur")
        void compteursParCategorie() {
            when(reportService.getAllStockRotation()).thenReturn(List.of(rotation()));
            when(reportService.getStockRotationCountByABCClassification()).thenReturn(
                Map.of(CategorieABC.A, 15L, CategorieABC.B, 40L, CategorieABC.C, 200L)
            );

            Map<String, Object> modele = modele();

            assertThat(modele.get("countA")).isEqualTo(15L);
            assertThat(modele.get("countB")).isEqualTo(40L);
            assertThat(modele.get("countC")).isEqualTo(200L);
        }

        @Test
        @DisplayName("une catégorie sans produit se lit zéro")
        void categorieAbsente() {
            when(reportService.getAllStockRotation()).thenReturn(List.of());
            when(reportService.getStockRotationCountByABCClassification()).thenReturn(Map.of(CategorieABC.A, 15L));

            Map<String, Object> modele = modele();

            assertThat(modele.get("countA")).isEqualTo(15L);
            assertThat(modele.get("countB")).isEqualTo(0L);
            assertThat(modele.get("countC")).isEqualTo(0L);
        }

        @Test
        @DisplayName("le détail des rotations et les mentions légales accompagnent le document")
        void modeleDuDocument() {
            when(reportService.getAllStockRotation()).thenReturn(List.of(rotation(), rotation()));
            when(reportService.getStockRotationCountByABCClassification()).thenReturn(Map.of());

            Map<String, Object> modele = modele();

            assertThat((List<?>) modele.get("rotations")).hasSize(2);
            assertThat(modele.get("reportTitle")).isEqualTo("Rapport de Rotation du Stock (Analyse ABC)");
            assertThat(modele.get("footer").toString()).contains("RC N° " + PdfReportTestSupport.REGISTRE);
        }

        private Map<String, Object> modele() {
            service.export();
            return service.getParameters();
        }

        private StockRotationDTO rotation() {
            return new StockRotationDTO(
                1, "DOLIPRANE", "CIP1", "MEDICAMENT", 25, 6_000, 150_000L, 30_000, 3, 1, 100_000, 10,
                BigDecimal.TEN, 90, CategorieABC.A
            );
        }
    }
}
