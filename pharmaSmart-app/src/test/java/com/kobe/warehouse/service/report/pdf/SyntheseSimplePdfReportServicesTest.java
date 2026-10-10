package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.domain.StockValuationView;
import com.kobe.warehouse.domain.enumeration.ClassePareto;
import com.kobe.warehouse.service.dto.report.ABCParetoDTO;
import com.kobe.warehouse.service.dto.report.ABCParetoSummaryDTO;
import com.kobe.warehouse.service.dto.report.MargeDTO;
import com.kobe.warehouse.service.dto.report.MargeSummaryDTO;
import com.kobe.warehouse.service.dto.report.StockValuationSummaryDTO;
import com.kobe.warehouse.service.dto.report.SupplierPerformanceDTO;
import com.kobe.warehouse.service.dto.report.SupplierPerformanceSummaryDTO;
import com.kobe.warehouse.service.report.ABCParetoReportService;
import com.kobe.warehouse.service.report.StockValuationReportService;
import com.kobe.warehouse.service.report.SupplierPerformanceReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Quatre documents ne calculent rien : ils reprennent une liste et son résumé, y posent un titre, et
 * s'impriment. Leur seul enjeu est donc de <b>ne rien perdre en chemin</b> — un détail ou un résumé
 * absent du modèle produit un document mis en page avec des blocs vides, sans la moindre erreur, et
 * le lecteur conclut que l'officine n'a pas de données plutôt qu'à un défaut d'impression.
 *
 * <p>La valorisation du stock fait exception : elle <b>choisit</b> son résumé selon que des filtres
 * ont été posés ou non. Un résumé non filtré posé à côté d'un détail filtré ferait afficher un total
 * supérieur à la somme des lignes imprimées.
 */
@DisplayName("Générateurs PDF de synthèse simple")
class SyntheseSimplePdfReportServicesTest {

    // ===== analyse ABC =====

    @Nested
    @DisplayName("Analyse ABC Pareto")
    class AnalyseAbc {

        private final ABCParetoReportService reportService = mock(ABCParetoReportService.class);
        private final ABCParetoPdfReportService service = new ABCParetoPdfReportService(
            PdfReportTestSupport.storageService(),
            PdfReportTestSupport.moteurDeGabarits(),
            reportService
        );

        @Test
        @DisplayName("le détail et le résumé de l'analyse accompagnent le document")
        void detailEtResume() {
            when(reportService.getAllABCParetoAnalysis()).thenReturn(List.of(produit("DOLIPRANE"), produit("EFFERALGAN")));
            when(reportService.getABCParetoSummary()).thenReturn(resumeAbc());

            service.export();
            Map<String, Object> modele = service.getParameters();

            assertThat((List<?>) modele.get("products")).hasSize(2);
            assertThat(modele).containsKey("summary");
            assertThat(modele.get("reportTitle")).isEqualTo("Analyse ABC Pareto (Règle 80/20)");
            assertThat(modele.get("footer").toString()).contains("RC N° " + PdfReportTestSupport.REGISTRE);
        }

        @Test
        @DisplayName("une analyse vide s'imprime tout de même")
        void analyseVide() {
            when(reportService.getAllABCParetoAnalysis()).thenReturn(List.of());
            when(reportService.getABCParetoSummary()).thenReturn(resumeAbc());

            service.export();

            assertThat((List<?>) service.getParameters().get("products")).isEmpty();
        }

        private ABCParetoDTO produit(String libelle) {
            return new ABCParetoDTO(
                1, libelle, "CIP", "MEDICAMENT", "A", 100_000, 10, 3, 2, 1_000_000L, 100_000L,
                BigDecimal.TEN, BigDecimal.TEN, 1, ClassePareto.A
            );
        }

        private ABCParetoSummaryDTO resumeAbc() {
            return new ABCParetoSummaryDTO(
                100, 10_000_000L, 5, 4_000_000L, BigDecimal.TEN, 10, 3_000_000L, BigDecimal.TEN,
                30, 2_000_000L, BigDecimal.TEN, 40, 900_000L, BigDecimal.TEN, 15, 100_000L, BigDecimal.TEN
            );
        }
    }

    // ===== rentabilité =====

    @Nested
    @DisplayName("Rentabilité")
    class Rentabilite {

        private final ProfitabilityPdfReportService service = new ProfitabilityPdfReportService(
            PdfReportTestSupport.storageService(),
            PdfReportTestSupport.moteurDeGabarits()
        );

        /**
         * Ce service reçoit ses données de l'appelant : il ne doit qu'en préserver l'intégralité.
         */
        @Test
        @DisplayName("les marges reçues et leur résumé accompagnent le document")
        void margesEtResume() {
            service.export(resumeMarge(), List.of(marge("DOLIPRANE"), marge("EFFERALGAN"), marge("ZOVIRAX")));

            Map<String, Object> modele = service.getParameters();

            assertThat((List<?>) modele.get("marges")).hasSize(3);
            assertThat(modele).containsKey("summary");
            assertThat(modele.get("reportTitle")).isEqualTo("Rapport de Rentabilité");
            assertThat(modele.get("footer").toString()).contains("RC N° " + PdfReportTestSupport.REGISTRE);
        }

        @Test
        @DisplayName("deux impressions successives ne cumulent pas leurs lignes")
        void deuxImpressionsSuccessives() {
            service.export(resumeMarge(), List.of(marge("A"), marge("B")));
            service.export(resumeMarge(), List.of(marge("C")));

            assertThat((List<?>) service.getParameters().get("marges")).hasSize(1);
        }

        private MargeDTO marge(String libelle) {
            return new MargeDTO(
                1, libelle, "CIP", "MEDICAMENT", 3, 10, 100_000L, 60_000L, 40_000L, BigDecimal.TEN,
                10_000, 6_000, 25, 6_000, 10_000, BigDecimal.TEN
            );
        }

        private MargeSummaryDTO resumeMarge() {
            return new MargeSummaryDTO(10, 1_000_000L, 600_000L, 400_000L, BigDecimal.TEN, 2, 100_000L, 5, 500_000L);
        }
    }

    // ===== performance fournisseurs =====

    @Nested
    @DisplayName("Performance fournisseurs")
    class PerformanceFournisseurs {

        private final SupplierPerformanceReportService reportService = mock(SupplierPerformanceReportService.class);
        private final SupplierPerformancePdfReportService service = new SupplierPerformancePdfReportService(
            PdfReportTestSupport.storageService(),
            PdfReportTestSupport.moteurDeGabarits(),
            reportService
        );

        @Test
        @DisplayName("le classement et son résumé accompagnent le document")
        void classementEtResume() {
            when(reportService.getAllSupplierPerformance()).thenReturn(List.of(fournisseur("LABOREX"), fournisseur("DPCI")));
            when(reportService.getSupplierPerformanceSummary()).thenReturn(resumeFournisseurs());

            service.export();
            Map<String, Object> modele = service.getParameters();

            assertThat((List<?>) modele.get("suppliers")).hasSize(2);
            assertThat(modele).containsKey("summary");
            assertThat(modele.get("reportTitle")).isEqualTo("Rapport de Performance Fournisseurs");
            assertThat(modele.get("footer").toString()).contains("RC N° " + PdfReportTestSupport.REGISTRE);
        }

        @Test
        @DisplayName("un classement vide s'imprime tout de même")
        void classementVide() {
            when(reportService.getAllSupplierPerformance()).thenReturn(List.of());
            when(reportService.getSupplierPerformanceSummary()).thenReturn(resumeFournisseurs());

            service.export();

            assertThat((List<?>) service.getParameters().get("suppliers")).isEmpty();
        }

        private SupplierPerformanceDTO fournisseur(String nom) {
            return new SupplierPerformanceDTO(
                1, nom, "F1", "0100000000", "0700000000", 2, 10_000_000L, 24, 120_000_000L, 3, 1, 10,
                BigDecimal.TEN, BigDecimal.TEN
            );
        }

        private SupplierPerformanceSummaryDTO resumeFournisseurs() {
            return new SupplierPerformanceSummaryDTO(
                3, 120_000_000L, 10_000_000L, 24, 2, BigDecimal.TEN, BigDecimal.TEN, 1, 1, 1
            );
        }
    }

    // ===== valorisation du stock =====

    @Nested
    @DisplayName("Valorisation du stock")
    class ValorisationDuStock {

        private final StockValuationReportService reportService = mock(StockValuationReportService.class);
        private final StockValuationPdfReportService service = new StockValuationPdfReportService(
            PdfReportTestSupport.storageService(),
            PdfReportTestSupport.moteurDeGabarits(),
            reportService
        );

        /**
         * Sans filtre, c'est le résumé global qui accompagne le détail. Prendre le résumé filtré
         * reviendrait à interroger la base pour un périmètre qu'on n'a pas restreint.
         */
        @Test
        @DisplayName("sans filtre, le résumé global accompagne le détail")
        void resumeGlobal() {
            when(reportService.getStockValuation(null, null)).thenReturn(List.of());
            when(reportService.getStockValuationSummary()).thenReturn(resumeValorisation());

            service.export(null, null);

            verify(reportService).getStockValuationSummary();
            verify(reportService, never()).getStockValuationSummary(null, null);
        }

        /**
         * Avec un filtre, c'est le résumé du même périmètre qui doit accompagner le détail : un total
         * global posé à côté de lignes filtrées annoncerait plus que ce que le document montre.
         */
        @Test
        @DisplayName("avec un filtre, le résumé suit le même périmètre que le détail")
        void resumeFiltre() {
            when(reportService.getStockValuation(7, null)).thenReturn(List.of());
            when(reportService.getStockValuationSummary(7, null)).thenReturn(resumeValorisation());

            service.export(7, null);

            verify(reportService).getStockValuationSummary(7, null);
            verify(reportService, never()).getStockValuationSummary();
        }

        @Test
        @DisplayName("le détail et le résumé accompagnent le document")
        void detailEtResume() {
            when(reportService.getStockValuation(null, null)).thenReturn(List.of(mock(StockValuationView.class)));
            when(reportService.getStockValuationSummary()).thenReturn(resumeValorisation());

            service.export(null, null);
            Map<String, Object> modele = service.getParameters();

            assertThat((List<?>) modele.get("valuations")).hasSize(1);
            assertThat(modele).containsKey("summary");
            assertThat(modele.get("reportTitle")).isEqualTo("Rapport de Valorisation du Stock");
            assertThat(modele.get("footer").toString()).contains("RC N° " + PdfReportTestSupport.REGISTRE);
        }

        private StockValuationSummaryDTO resumeValorisation() {
            return new StockValuationSummaryDTO(600_000L, 1_000_000L, 400_000L, BigDecimal.TEN, 25, 890);
        }
    }
}
