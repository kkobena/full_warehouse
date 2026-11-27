package com.kobe.warehouse.service.exports.impl;

import static org.mockito.Mockito.verify;

import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.RapportExport;
import com.kobe.warehouse.service.remise.JournalRemisesService;
import com.kobe.warehouse.service.remise.dto.JournalRemisesFiltre;
import com.kobe.warehouse.service.remise.dto.NatureJournal;
import com.kobe.warehouse.service.report.MargeReportService;
import com.kobe.warehouse.service.report.excel.DashboardCAPExcelCsvExportService;
import com.kobe.warehouse.service.report.pdf.ComparativePdfReportService;
import com.kobe.warehouse.service.report.pdf.CustomerSegmentationPdfReportService;
import com.kobe.warehouse.service.report.pdf.DashboardCAPdfExportService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Chaque format sort du service que l'écran appelle, et les réglages y arrivent tels quels. */
@ExtendWith(MockitoExtension.class)
@DisplayName("Exports — éditions des rapports de ventes")
class ProducteurRapportsVentesTest {

    private static final LocalDate DU = LocalDate.of(2026, 10, 1);
    private static final LocalDate AU = LocalDate.of(2026, 10, 31);

    @Mock
    private DashboardCAPdfExportService dashboardCAPdfExportService;

    @Mock
    private DashboardCAPExcelCsvExportService dashboardCAPExcelCsvExportService;

    @Mock
    private ComparativePdfReportService comparativePdfReportService;

    @Mock
    private MargeReportService margeReportService;

    @Mock
    private CustomerSegmentationPdfReportService customerSegmentationPdfReportService;

    @Mock
    private JournalRemisesService journalRemisesService;

    @InjectMocks
    private ProducteurRapportsVentes ventes;

    @Test
    @DisplayName("tableau de bord du CA : un service par format")
    void tableauDeBordCa() throws Exception {
        ventes.produire(RapportExport.TABLEAU_DE_BORD_CA, FormatExport.PDF, DU, AU, Map.of());
        ventes.produire(RapportExport.TABLEAU_DE_BORD_CA, FormatExport.XLSX, DU, AU, Map.of());
        ventes.produire(RapportExport.TABLEAU_DE_BORD_CA, FormatExport.CSV, DU, AU, Map.of());

        verify(dashboardCAPdfExportService).export(DU, AU);
        verify(dashboardCAPExcelCsvExportService).exportDailySummaryToExcel(DU, AU);
        verify(dashboardCAPExcelCsvExportService).exportDailySummaryToCsv(DU, AU);
    }

    @Test
    @DisplayName("meilleures ventes : en tableur seulement, Excel par défaut")
    void meilleuresVentes() throws Exception {
        ventes.produire(RapportExport.MEILLEURES_VENTES, FormatExport.XLSX, DU, AU, Map.of());
        ventes.produire(RapportExport.MEILLEURES_VENTES, FormatExport.CSV, DU, AU, Map.of());

        verify(dashboardCAPExcelCsvExportService).exportTopProductsToExcel(DU, AU);
        verify(dashboardCAPExcelCsvExportService).exportTopProductsToCsv(DU, AU);
    }

    @Test
    @DisplayName("comparatif : mois par mois sans réglage, sur l'année en cours")
    void comparatifParDefaut() {
        ventes.produire(RapportExport.COMPARATIF_VENTES, FormatExport.PDF, null, null, Map.of());

        verify(comparativePdfReportService).export("MONTHLY", LocalDate.now().getYear());
    }

    @Test
    @DisplayName("comparatif : le découpage retenu part tel quel")
    void comparatifRegle() {
        ventes.produire(
                RapportExport.COMPARATIF_VENTES,
                FormatExport.PDF,
                null,
                null,
                Map.of(FiltreExport.DECOUPAGE_COMPARATIF, List.of("YEARLY")));

        verify(comparativePdfReportService).export("YEARLY", LocalDate.now().getYear());
    }

    @Test
    @DisplayName("rentabilité : la famille retenue, une seule, sans recherche")
    void rentabilite() {
        ventes.produire(
                RapportExport.RENTABILITE_PRODUITS,
                FormatExport.PDF,
                null,
                null,
                Map.of(FiltreExport.FAMILLE_UNIQUE, List.of("12")));

        verify(margeReportService).export(12, null);
    }

    @Test
    @DisplayName("segmentation des clients : état du jour, sans paramètre")
    void segmentation() {
        ventes.produire(RapportExport.SEGMENTATION_CLIENTS, FormatExport.PDF, null, null, Map.of());

        verify(customerSegmentationPdfReportService).export();
    }

    @Test
    @DisplayName("journal des remises : sans partie choisie, la synthèse et les trois journaux")
    void journalRemisesComplet() {
        ventes.produire(RapportExport.JOURNAL_REMISES, FormatExport.PDF, DU, AU, Map.of());

        verify(journalRemisesService).exporter(FormatExport.PDF, null, JournalRemisesFiltre.periode(DU, AU));
    }

    @Test
    @DisplayName("journal des remises : la partie et les filtres retenus partent tels quels, au type du service")
    void journalRemisesRegle() {
        ventes.produire(
                RapportExport.JOURNAL_REMISES,
                FormatExport.XLSX,
                DU,
                AU,
                Map.of(
                        FiltreExport.JOURNAL, List.of("UG"),
                        FiltreExport.NATURE_VENTE, List.of("ASSURANCE"),
                        FiltreExport.CAISSIER, List.of("7"),
                        FiltreExport.VENDEUR, List.of("8"),
                        FiltreExport.PROMOTION, List.of("42")));

        verify(journalRemisesService)
                .exporter(
                        FormatExport.XLSX,
                        NatureJournal.UG,
                        new JournalRemisesFiltre(
                                DU, AU, Set.of(7), Set.of(8), Set.of(NatureVente.ASSURANCE), Set.of(42L), null, null));
    }
}
