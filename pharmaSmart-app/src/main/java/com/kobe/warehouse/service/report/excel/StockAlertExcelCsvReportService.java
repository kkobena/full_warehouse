package com.kobe.warehouse.service.report.excel;

import com.kobe.warehouse.domain.enumeration.StockAlertType;
import com.kobe.warehouse.service.dto.report.StockAlertDTO;
import com.kobe.warehouse.service.report.StockAlertReportService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockAlertExcelCsvReportService {
    private final ReportExcelExportService excelExportService;
    private final CsvExportService csvExportService;
    private final StockAlertReportService stockAlertReportService;

    public StockAlertExcelCsvReportService(ReportExcelExportService excelExportService, CsvExportService csvExportService, StockAlertReportService stockAlertReportService) {
        this.excelExportService = excelExportService;
        this.csvExportService = csvExportService;
        this.stockAlertReportService = stockAlertReportService;
    }

    public byte[] exportToExcel(List<StockAlertType> alertTypes) throws Exception {
        List<StockAlertDTO> data = stockAlertReportService.getStockAlerts(alertTypes, Pageable.unpaged()).getContent();

        String title = "Alertes Stock";
        if (alertTypes != null && !alertTypes.isEmpty()) {
            title += " - " + alertTypes.stream().map(Enum::name).collect(Collectors.joining(", "));
        }

        String[] headers = {
            "Code CIP",
            "Libellé",
            "Stock Actuel",
            "Seuil Min",
            "Date Péremption",
            "Type d'Alerte"
        };

        return excelExportService.createExcelReport(title, headers, data, (row, dto) -> {
            row.createCell(0).setCellValue(dto.codeCip() != null ? dto.codeCip() : "");
            row.createCell(1).setCellValue(dto.libelle() != null ? dto.libelle() : "");
            row.createCell(2).setCellValue(dto.stockQuantity() != null ? dto.stockQuantity() : 0);
            row.createCell(3).setCellValue(dto.seuilMin() != null ? dto.seuilMin() : 0);
            row.createCell(4).setCellValue(dto.expiryDate() != null ? dto.expiryDate().toString() : "");
            row.createCell(5).setCellValue(dto.alertType() != null ? dto.alertType().name() : "");
        });
    }


    public byte[] exportToCsv(List<StockAlertType> alertTypes) throws Exception {
        List<StockAlertDTO> data = stockAlertReportService.getStockAlerts(alertTypes, Pageable.unpaged()).getContent();

        String title = "Alertes Stock";
        if (alertTypes != null && !alertTypes.isEmpty()) {
            title += " - " + alertTypes.stream().map(Enum::name).collect(Collectors.joining(", "));
        }

        String[] headers = {
            "Code CIP",
            "Libellé",
            "Stock Actuel",
            "Seuil Min",
            "Date Péremption",
            "Type d'Alerte"
        };

        byte[] csvData = csvExportService.createCsvReport(title, headers, data, dto -> new String[] {
            dto.codeCip() != null ? dto.codeCip() : "",
            dto.libelle() != null ? dto.libelle() : "",
            String.valueOf(dto.stockQuantity() != null ? dto.stockQuantity() : 0),
            String.valueOf(dto.seuilMin() != null ? dto.seuilMin() : 0),
            dto.expiryDate() != null ? dto.expiryDate().toString() : "",
            dto.alertType() != null ? dto.alertType().name() : ""
        });

        return csvExportService.addUtf8Bom(csvData);
    }
}
