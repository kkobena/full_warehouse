package com.kobe.warehouse.service.report.excel;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.service.sale.SaleDataService;
import com.kobe.warehouse.service.stock.dto.StockDepotExportDTO;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StockDepotExcelCsvReportService {

    private static final String[] HEADERS = {
        "Produit ID",
        "Code CIP",
        "Produit",
        "Code EAN",
        "Qté vendue",
        "Qté demandée",
        "Prix unitaire",
        "Valeur taxe",
        "Prix achat"
    };

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final ReportExcelExportService excelExportService;
    private final CsvExportService csvExportService;
    private final SaleDataService saleDataService;

    public StockDepotExcelCsvReportService(
        ReportExcelExportService excelExportService,
        CsvExportService csvExportService,
        SaleDataService saleDataService
    ) {
        this.excelExportService = excelExportService;
        this.csvExportService = csvExportService;
        this.saleDataService = saleDataService;
    }

    public byte[] exportToExcel(SaleId saleId) throws IOException {
        List<StockDepotExportDTO> data = saleDataService.exportVenteDepotStock(saleId);
        String title = buildReportTitle(saleId);

        return excelExportService.createExcelReport(title, HEADERS, data, (row, dto) -> {
            row.createCell(0).setCellValue(dto.getProduitId() != null ? dto.getProduitId() : 0);
            row.createCell(1).setCellValue(dto.getCode() != null ? dto.getCode() : "");
            row.createCell(2).setCellValue(dto.getProduitLibelle() != null ? dto.getProduitLibelle() : "");
            row.createCell(3).setCellValue(dto.getCodeEan() != null ? dto.getCodeEan() : "");
            row.createCell(4).setCellValue(dto.getQuantitySold() != null ? dto.getQuantitySold() : 0);
            row.createCell(5).setCellValue(dto.getQuantityRequested() != null ? dto.getQuantityRequested() : 0);
            row.createCell(6).setCellValue(dto.getRegularUnitPrice() != null ? dto.getRegularUnitPrice() / 100.0 : 0);
            row.createCell(7).setCellValue(dto.getTaxValue() != null ? dto.getTaxValue() / 100.0 : 0);
            row.createCell(8).setCellValue(dto.getCostAmount() != null ? dto.getCostAmount() / 100.0 : 0);
        });
    }

    public byte[] exportToCsv(SaleId saleId) throws IOException {
        List<StockDepotExportDTO> data = saleDataService.exportVenteDepotStock(saleId);
        String title = buildReportTitle(saleId);

        byte[] csvData = csvExportService.createCsvReport(title, HEADERS, data, dto -> new String[] {
            String.valueOf(dto.getProduitId() != null ? dto.getProduitId() : 0),
            dto.getCode() != null ? dto.getCode() : "",
            dto.getProduitLibelle() != null ? dto.getProduitLibelle() : "",
            dto.getCodeEan() != null ? dto.getCodeEan() : "",
            String.valueOf(dto.getQuantitySold() != null ? dto.getQuantitySold() : 0),
            String.valueOf(dto.getQuantityRequested() != null ? dto.getQuantityRequested() : 0),
            String.format("%.2f", dto.getRegularUnitPrice() != null ? dto.getRegularUnitPrice() / 100.0 : 0),
            String.format("%.2f", dto.getTaxValue() != null ? dto.getTaxValue() / 100.0 : 0),
            String.format("%.2f", dto.getCostAmount() != null ? dto.getCostAmount() / 100.0 : 0)
        });

        return csvExportService.addUtf8Bom(csvData);
    }

    private String buildReportTitle(SaleId saleId) {
        return "Vente Dépôt Stock - " + saleId.getId() + " du " + saleId.getSaleDate().format(DATE_FORMATTER);
    }
}
