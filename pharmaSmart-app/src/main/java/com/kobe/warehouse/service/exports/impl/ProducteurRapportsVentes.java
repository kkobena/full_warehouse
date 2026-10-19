package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.RapportExport;
import com.kobe.warehouse.service.report.MargeReportService;
import com.kobe.warehouse.service.report.excel.DashboardCAPExcelCsvExportService;
import com.kobe.warehouse.service.report.pdf.ComparativePdfReportService;
import com.kobe.warehouse.service.report.pdf.CustomerSegmentationPdfReportService;
import com.kobe.warehouse.service.report.pdf.DashboardCAPdfExportService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Éditions des écrans Rapports de ventes et Clients, bâties par leur propre service. */
@Component
class ProducteurRapportsVentes {

    /** Le découpage que l'écran Comparatif propose par défaut ; {@code ComparativePdfReportService} attend ce mot-clé. */
    private static final String DECOUPAGE_COMPARATIF_PAR_DEFAUT = "MONTHLY";

    private final DashboardCAPdfExportService dashboardCAPdfExportService;
    private final DashboardCAPExcelCsvExportService dashboardCAPExcelCsvExportService;
    private final ComparativePdfReportService comparativePdfReportService;
    private final MargeReportService margeReportService;
    private final CustomerSegmentationPdfReportService customerSegmentationPdfReportService;

    ProducteurRapportsVentes(
        DashboardCAPdfExportService dashboardCAPdfExportService,
        DashboardCAPExcelCsvExportService dashboardCAPExcelCsvExportService,
        ComparativePdfReportService comparativePdfReportService,
        MargeReportService margeReportService,
        CustomerSegmentationPdfReportService customerSegmentationPdfReportService
    ) {
        this.dashboardCAPdfExportService = dashboardCAPdfExportService;
        this.dashboardCAPExcelCsvExportService = dashboardCAPExcelCsvExportService;
        this.comparativePdfReportService = comparativePdfReportService;
        this.margeReportService = margeReportService;
        this.customerSegmentationPdfReportService = customerSegmentationPdfReportService;
    }

    byte[] produire(RapportExport rapport, FormatExport format, LocalDate du, LocalDate au, Map<FiltreExport, List<String>> filtres) {
        return switch (rapport) {
            case TABLEAU_DE_BORD_CA -> produireTableauDeBordCa(format, du, au);
            case MEILLEURES_VENTES -> produireMeilleuresVentes(format, du, au);
            case COMPARATIF_VENTES -> comparativePdfReportService.export(lireDecoupage(filtres), LocalDate.now().getYear());
            case RENTABILITE_PRODUITS -> margeReportService.export(ReglagesRapports.lireEntier(filtres, FiltreExport.FAMILLE_UNIQUE), null);
            case SEGMENTATION_CLIENTS -> customerSegmentationPdfReportService.export();
            default -> throw new IllegalArgumentException("Édition hors des rapports de ventes : " + rapport);
        };
    }

    private byte[] produireTableauDeBordCa(FormatExport format, LocalDate du, LocalDate au) {
        return switch (format) {
            case PDF -> dashboardCAPdfExportService.export(du, au);
            case XLSX -> ProductionVerifiee.tenter(() -> dashboardCAPExcelCsvExportService.exportDailySummaryToExcel(du, au));
            case CSV -> ProductionVerifiee.tenter(() -> dashboardCAPExcelCsvExportService.exportDailySummaryToCsv(du, au));
        };
    }

    /** Les meilleures ventes ne sortent qu'en tableur : l'écran n'en produit pas de PDF. */
    private byte[] produireMeilleuresVentes(FormatExport format, LocalDate du, LocalDate au) {
        return format == FormatExport.CSV
            ? ProductionVerifiee.tenter(() -> dashboardCAPExcelCsvExportService.exportTopProductsToCsv(du, au))
            : ProductionVerifiee.tenter(() -> dashboardCAPExcelCsvExportService.exportTopProductsToExcel(du, au));
    }

    /** Sans choix, le découpage par défaut de l'écran ; l'année comparée est l'année en cours, comme lui. */
    private static String lireDecoupage(Map<FiltreExport, List<String>> filtres) {
        String decoupage = ReglagesRapports.lireTexte(filtres, FiltreExport.DECOUPAGE_COMPARATIF);
        return decoupage == null ? DECOUPAGE_COMPARATIF_PAR_DEFAUT : decoupage;
    }
}
