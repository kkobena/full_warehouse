package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.RapportExport;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyFilter;
import com.kobe.warehouse.service.product_to_destroy.service.ProductsToDestroyService;
import com.kobe.warehouse.service.report.excel.ProductToDestroyExcelCsvReportService;
import com.kobe.warehouse.service.report.excel.StockAlertExcelCsvReportService;
import com.kobe.warehouse.service.report.pdf.ABCParetoPdfReportService;
import com.kobe.warehouse.service.report.pdf.StockAlertPdfReportService;
import com.kobe.warehouse.service.report.pdf.StockRotationPdfReportService;
import com.kobe.warehouse.service.report.pdf.StockValuationPdfReportService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Éditions des écrans Rapports de stock. Elles ne portent pas de période : ce sont des états du stock au moment où on les
 * demande, exactement comme à l'écran.
 */
@Component
class ProducteurRapportsStock {

    private final StockAlertPdfReportService stockAlertPdfReportService;
    private final StockAlertExcelCsvReportService stockAlertExcelCsvReportService;
    private final StockRotationPdfReportService stockRotationPdfReportService;
    private final StockValuationPdfReportService stockValuationPdfReportService;
    private final ABCParetoPdfReportService abcParetoPdfReportService;
    private final ProductsToDestroyService productsToDestroyService;
    private final ProductToDestroyExcelCsvReportService productToDestroyExcelCsvReportService;

    ProducteurRapportsStock(
        StockAlertPdfReportService stockAlertPdfReportService,
        StockAlertExcelCsvReportService stockAlertExcelCsvReportService,
        StockRotationPdfReportService stockRotationPdfReportService,
        StockValuationPdfReportService stockValuationPdfReportService,
        ABCParetoPdfReportService abcParetoPdfReportService,
        ProductsToDestroyService productsToDestroyService,
        ProductToDestroyExcelCsvReportService productToDestroyExcelCsvReportService
    ) {
        this.stockAlertPdfReportService = stockAlertPdfReportService;
        this.stockAlertExcelCsvReportService = stockAlertExcelCsvReportService;
        this.stockRotationPdfReportService = stockRotationPdfReportService;
        this.stockValuationPdfReportService = stockValuationPdfReportService;
        this.abcParetoPdfReportService = abcParetoPdfReportService;
        this.productsToDestroyService = productsToDestroyService;
        this.productToDestroyExcelCsvReportService = productToDestroyExcelCsvReportService;
    }

    byte[] produire(RapportExport rapport, FormatExport format, LocalDate du, LocalDate au, Map<FiltreExport, List<String>> filtres) {
        return switch (rapport) {
            // Aucun type retenu : toutes les alertes, ruptures comprises, comme le défaut de l'écran.
            case ALERTES_STOCK -> produireAlertes(format);
            case ROTATION_STOCK -> stockRotationPdfReportService.export();
            case VALORISATION_STOCK -> stockValuationPdfReportService.export(
                ReglagesRapports.lireEntier(filtres, FiltreExport.FAMILLE_UNIQUE),
                null
            );
            case ABC_PARETO -> abcParetoPdfReportService.export();
            case PRODUITS_A_DETRUIRE -> produireADetruire(format, du, au);
            default -> throw new IllegalArgumentException("Édition hors des rapports de stock : " + rapport);
        };
    }

    /**
     * Les produits retirés sur la période, détruits ou non : l'écran ne distingue que par ses propres filtres, qu'une période
     * suffit à remplacer ici.
     */
    private byte[] produireADetruire(FormatExport format, LocalDate du, LocalDate au) {
        ProductToDestroyFilter filtre = new ProductToDestroyFilter(du, au, null, null, null, null, null, null, null);
        return switch (format) {
            case PDF -> productsToDestroyService.generatePdf(filtre).getBody();
            case XLSX -> ProductionVerifiee.tenter(() -> productToDestroyExcelCsvReportService.exportToExcel(filtre));
            case CSV -> ProductionVerifiee.tenter(() -> productToDestroyExcelCsvReportService.exportToCsv(filtre));
        };
    }

    private byte[] produireAlertes(FormatExport format) {
        return switch (format) {
            case PDF -> stockAlertPdfReportService.export(null);
            case XLSX -> ProductionVerifiee.tenter(() -> stockAlertExcelCsvReportService.exportToExcel(null));
            case CSV -> ProductionVerifiee.tenter(() -> stockAlertExcelCsvReportService.exportToCsv(null));
        };
    }
}
