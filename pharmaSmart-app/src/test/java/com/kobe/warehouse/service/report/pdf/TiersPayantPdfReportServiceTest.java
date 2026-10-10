package com.kobe.warehouse.service.report.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.service.dto.report.TiersPayantCreancesSummaryDTO;
import com.kobe.warehouse.service.dto.report.TiersPayantInvoiceDTO;
import com.kobe.warehouse.service.report.TiersPayantReportService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le rapport des créances tiers payants est le document qu'on présente en réunion de trésorerie :
 * combien chaque assureur doit, et le détail des factures non soldées. Son total est ce qu'on
 * annonce à voix haute — l'argent que l'officine a avancé et qui n'est pas rentré.
 *
 * <p>Ce total n'est pas fourni : il est recalculé sur la synthèse par tiers payant, et doit donc
 * correspondre à la somme des lignes imprimées juste en dessous.
 */
@DisplayName("TiersPayantPdfReportService — rapport des créances tiers payants")
class TiersPayantPdfReportServiceTest {

    private TiersPayantReportService tiersPayantReportService;
    private TiersPayantPdfReportService service;

    @BeforeEach
    void setUp() {
        tiersPayantReportService = mock(TiersPayantReportService.class);
        when(tiersPayantReportService.getCreancesSummary()).thenReturn(List.of());
        when(tiersPayantReportService.getUnpaidInvoices(any(), any())).thenReturn(List.of());

        service = new TiersPayantPdfReportService(
            PdfReportTestSupport.storageService(),
            PdfReportTestSupport.moteurDeGabarits(),
            tiersPayantReportService
        );
    }

    @Test
    @DisplayName("le total des créances additionne les dettes de chaque tiers payant")
    void totalDesCreances() {
        when(tiersPayantReportService.getCreancesSummary()).thenReturn(
            List.of(creance("MUGEFCI", 3_000_000), creance("CNPS", 1_500_000), creance("ASACI", 500_000))
        );

        assertThat(modele().get("totalCreances")).isEqualTo(5_000_000);
    }

    @Test
    @DisplayName("un montant absent est traité comme zéro")
    void montantAbsent() {
        when(tiersPayantReportService.getCreancesSummary()).thenReturn(List.of(creance("MUGEFCI", null)));

        assertThat(modele().get("totalCreances")).isEqualTo(0);
    }

    /** Le détail des factures accompagne la synthèse : c'est lui qu'on oppose au tiers payant. */
    @Test
    @DisplayName("la synthèse et le détail des factures impayées sont joints au document")
    void syntheseEtDetail() {
        when(tiersPayantReportService.getCreancesSummary()).thenReturn(List.of(creance("MUGEFCI", 3_000_000)));
        when(tiersPayantReportService.getUnpaidInvoices(any(), any())).thenReturn(List.of(facture(), facture()));

        Map<String, Object> modele = modele();

        assertThat((List<?>) modele.get("summary")).hasSize(1);
        assertThat((List<?>) modele.get("invoices")).hasSize(2);
        assertThat(modele.get("reportTitle")).isEqualTo("Rapport Créances Tiers-Payants");
        assertThat(modele.get("footer").toString()).contains("RC N° " + PdfReportTestSupport.REGISTRE);
    }

    @Test
    @DisplayName("sans créance, le document s'imprime avec un total nul")
    void aucuneCreance() {
        assertThat(modele().get("totalCreances")).isEqualTo(0);
        assertThat((List<?>) modele().get("summary")).isEmpty();
    }

    // ===== fabriques =====

    private Map<String, Object> modele() {
        service.export();
        return service.getParameters();
    }

    private static TiersPayantCreancesSummaryDTO creance(String nom, Integer montantTotal) {
        return new TiersPayantCreancesSummaryDTO(1, nom, 1, montantTotal, 0, 0, 0, 0);
    }

    private static TiersPayantInvoiceDTO facture() {
        return new TiersPayantInvoiceDTO(
            1L,
            "2026_0001",
            null,
            "MUGEFCI",
            null,
            100_000,
            0,
            100_000,
            TiersPayantInvoiceDTO.InvoiceStatus.UNPAID,
            10,
            TiersPayantInvoiceDTO.AgeCategory.LESS_THAN_30
        );
    }
}
