package com.kobe.warehouse.service.report.excel;

import com.kobe.warehouse.service.dto.report.DailyCADTO;
import com.kobe.warehouse.service.dto.report.TopProductDTO;
import com.kobe.warehouse.service.report.DashboardCAService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardCAPExcelCsvExportService {
    private final ReportExcelExportService excelExportService;
    private final CsvExportService csvExportService;
    private final DashboardCAService dashboardCAService;

    public DashboardCAPExcelCsvExportService(ReportExcelExportService excelExportService, CsvExportService csvExportService, DashboardCAService dashboardCAService) {
        this.excelExportService = excelExportService;
        this.csvExportService = csvExportService;
        this.dashboardCAService = dashboardCAService;
    }


    public byte[] exportDailySummaryToExcel(LocalDate startDate, LocalDate endDate) throws Exception {
        List<DailyCADTO> data = dashboardCAService.getDailySummary(startDate, endDate);

        String title = "Chiffre d'Affaires Journalier - " + startDate + " au " + endDate;
        String[] headers = {
            "Date",
            "Nb Trans.",
            "Nb Avoirs",
            "CA Total",
            "CA Avoirs",
            "CA Net",
            "Panier Moyen",
            "Coût Total",
            "Marge Brute",
            "Taux Marge %",
            "Nb Clients",
            "Encaissé",
            "Crédit"
        };

        return excelExportService.createExcelReport(title, headers, data, (row, dto) -> {
            row.createCell(0).setCellValue(dto.saleDate().toString());
            row.createCell(1).setCellValue(dto.nbTransactions());

            row.createCell(2).setCellValue(dto.caTotal() / 100.0);
            row.createCell(3).setCellValue(dto.caNet() / 100.0);
            row.createCell(4).setCellValue(dto.panierMoyen().doubleValue());
            row.createCell(5).setCellValue(dto.coutTotal() / 100.0);
            row.createCell(6).setCellValue(dto.margeBrute() / 100.0);
            row.createCell(7).setCellValue(dto.tauxMargePct().doubleValue());
            row.createCell(8).setCellValue(dto.nbClients());
            row.createCell(9).setCellValue(dto.montantEncaisse() / 100.0);
            row.createCell(10).setCellValue(dto.montantCredit() / 100.0);
        });
    }


    public byte[] exportDailySummaryToCsv(LocalDate startDate, LocalDate endDate) throws Exception {
        List<DailyCADTO> data = dashboardCAService.getDailySummary(startDate, endDate);

        String title = "Chiffre d'Affaires Journalier - " + startDate + " au " + endDate;
        String[] headers = {
            "Date",
            "Nb Transactions",
            "Nb Avoirs",
            "CA Total",
            "CA Avoirs",
            "CA Net",
            "Panier Moyen",
            "Coût Total",
            "Marge Brute",
            "Taux Marge %",
            "Nb Clients",
            "Encaissé",
            "Crédit"
        };

        byte[] csvData = csvExportService.createCsvReport(title, headers, data, dto -> new String[] {
            dto.saleDate().toString(),
            String.valueOf(dto.nbTransactions()),
            String.format("%.2f", dto.caTotal() / 100.0),
            String.format("%.2f", dto.caNet() / 100.0),
            String.format("%.2f", dto.panierMoyen()),
            String.format("%.2f", dto.coutTotal() / 100.0),
            String.format("%.2f", dto.margeBrute() / 100.0),
            String.format("%.2f", dto.tauxMargePct()),
            String.valueOf(dto.nbClients()),
            String.format("%.2f", dto.montantEncaisse() / 100.0),
            String.format("%.2f", dto.montantCredit() / 100.0)
        });

        return csvExportService.addUtf8Bom(csvData);
    }


    public byte[] exportTopProductsToExcel(LocalDate startDate, LocalDate endDate) throws Exception {
        List<TopProductDTO> data = dashboardCAService.getTopProducts(startDate, endDate, 50);

        String title = "Top Produits par CA - " + startDate + " au " + endDate;
        String[] headers = { "Code CIP", "Libellé", "Nb Ventes", "Qté Vendue", "CA Généré", "Prix Moyen"};

        return excelExportService.createExcelReport(title, headers, data, (row, dto) -> {
            row.createCell(0).setCellValue(dto.codeCip() != null ? dto.codeCip() : "");
            row.createCell(1).setCellValue(dto.libelle() != null ? dto.libelle() : "");
            row.createCell(2).setCellValue(dto.nbVentes() != null ? dto.nbVentes() : 0);
            row.createCell(3).setCellValue(dto.qteVendue() != null ? dto.qteVendue() : 0);
            row.createCell(4).setCellValue(dto.caGenere() != null ? dto.caGenere() / 100.0 : 0);
            row.createCell(5).setCellValue(dto.prixMoyen() != null ? dto.prixMoyen().doubleValue() : 0);
        });
    }


    public byte[] exportTopProductsToCsv(LocalDate startDate, LocalDate endDate) throws Exception {
        List<TopProductDTO> data = dashboardCAService.getTopProducts(startDate, endDate, 50);

        String title = "Top Produits par CA - " + startDate + " au " + endDate;
        String[] headers = { "Code CIP", "Libellé", "Nb Ventes", "Qté Vendue", "CA Généré", "Prix Moyen"};

        byte[] csvData = csvExportService.createCsvReport(title, headers, data, dto -> new String[] {
            dto.codeCip() != null ? dto.codeCip() : "",
            dto.libelle() != null ? dto.libelle() : "",
            String.valueOf(dto.nbVentes() != null ? dto.nbVentes() : 0),
            String.valueOf(dto.qteVendue() != null ? dto.qteVendue() : 0),
            String.format("%.2f", dto.caGenere() != null ? dto.caGenere() / 100.0 : 0),
            String.format("%.2f", dto.prixMoyen() != null ? dto.prixMoyen() : BigDecimal.ZERO)
        });

        return csvExportService.addUtf8Bom(csvData);
    }
}
