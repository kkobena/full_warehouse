package com.kobe.warehouse.service.exports.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

/** Les états de stock ne portent pas de période : ce sont des photos du moment, comme à l'écran. */
@ExtendWith(MockitoExtension.class)
@DisplayName("Exports — éditions des rapports de stock")
class ProducteurRapportsStockTest {

    @Mock
    private StockAlertPdfReportService stockAlertPdfReportService;

    @Mock
    private StockAlertExcelCsvReportService stockAlertExcelCsvReportService;

    @Mock
    private StockRotationPdfReportService stockRotationPdfReportService;

    @Mock
    private StockValuationPdfReportService stockValuationPdfReportService;

    @Mock
    private ABCParetoPdfReportService abcParetoPdfReportService;

    @Mock
    private ProductsToDestroyService productsToDestroyService;

    @Mock
    private ProductToDestroyExcelCsvReportService productToDestroyExcelCsvReportService;

    @Captor
    private ArgumentCaptor<ProductToDestroyFilter> filtreDestruction;

    @InjectMocks
    private ProducteurRapportsStock stock;

    @Test
    @DisplayName("alertes de stock : un service par format, tous les types d'alerte")
    void alertes() throws Exception {
        stock.produire(RapportExport.ALERTES_STOCK, FormatExport.PDF, null, null, Map.of());
        stock.produire(RapportExport.ALERTES_STOCK, FormatExport.XLSX, null, null, Map.of());
        stock.produire(RapportExport.ALERTES_STOCK, FormatExport.CSV, null, null, Map.of());

        verify(stockAlertPdfReportService).export(null);
        verify(stockAlertExcelCsvReportService).exportToExcel(null);
        verify(stockAlertExcelCsvReportService).exportToCsv(null);
    }

    @Test
    @DisplayName("valorisation : la famille retenue, sans rayon")
    void valorisation() {
        stock.produire(RapportExport.VALORISATION_STOCK, FormatExport.PDF, null, null, Map.of(FiltreExport.FAMILLE_UNIQUE, List.of("12")));

        verify(stockValuationPdfReportService).export(12, null);
    }

    @Test
    @DisplayName("rotation et ABC/Pareto : sans paramètre")
    void rotationEtAbc() {
        stock.produire(RapportExport.ROTATION_STOCK, FormatExport.PDF, null, null, Map.of());
        stock.produire(RapportExport.ABC_PARETO, FormatExport.PDF, null, null, Map.of());

        verify(stockRotationPdfReportService).export();
        verify(abcParetoPdfReportService).export();
    }

    @Test
    @DisplayName("produits à détruire : la période demandée, détruits ou non")
    void produitsADetruire() throws Exception {
        LocalDate du = LocalDate.of(2026, 10, 1);
        LocalDate au = LocalDate.of(2026, 10, 31);
        when(productsToDestroyService.generatePdf(any())).thenReturn(ResponseEntity.ok(new byte[0]));

        stock.produire(RapportExport.PRODUITS_A_DETRUIRE, FormatExport.PDF, du, au, Map.of());
        stock.produire(RapportExport.PRODUITS_A_DETRUIRE, FormatExport.CSV, du, au, Map.of());

        verify(productsToDestroyService).generatePdf(filtreDestruction.capture());
        ProductToDestroyFilter filtre = filtreDestruction.getValue();
        assertThat(filtre.fromDate()).isEqualTo(du);
        assertThat(filtre.toDate()).isEqualTo(au);
        assertThat(filtre.destroyed()).isNull();
        verify(productToDestroyExcelCsvReportService).exportToCsv(any());
    }
}
