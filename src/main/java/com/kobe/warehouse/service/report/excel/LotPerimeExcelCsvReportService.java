package com.kobe.warehouse.service.report.excel;

import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.dto.LotFilterParam;
import com.kobe.warehouse.service.stock.dto.LotPerimeDTO;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LotPerimeExcelCsvReportService {

    private static final String[] HEADERS = {
        "Numéro de lot",
        "Fournisseur",
        "Nom du produit",
        "Code du produit",
        "Date de péremption",
        "Quantité",
        "Prix d'achat",
        "Prix de vente",
        "Prix total de vente",
        "Prix total d'achat",
        "Statut de péremption",
        "Nom du rayon",
        "Famille du produit"
    };

    private final ReportExcelExportService excelExportService;
    private final CsvExportService csvExportService;
    private final LotService lotService;

    public LotPerimeExcelCsvReportService(
        ReportExcelExportService excelExportService,
        CsvExportService csvExportService,
        LotService lotService
    ) {
        this.excelExportService = excelExportService;
        this.csvExportService = csvExportService;
        this.lotService = lotService;
    }

    public byte[] exportToExcel(LotFilterParam lotFilterParam) throws IOException {
        List<LotPerimeDTO> data = lotService.findLotsPerimes(lotFilterParam, Pageable.unpaged()).getContent();
        String title = buildReportTitle(lotFilterParam.getFromDate(), lotFilterParam.getToDate());

        return excelExportService.createExcelReport(title, HEADERS, data, (row, dto) -> {
            row.createCell(0).setCellValue(dto.getNumLot() != null ? dto.getNumLot() : "");
            row.createCell(1).setCellValue(dto.getFournisseur() != null ? dto.getFournisseur() : "");
            row.createCell(2).setCellValue(dto.getProduitName() != null ? dto.getProduitName() : "");
            row.createCell(3).setCellValue(dto.getProduitCode() != null ? dto.getProduitCode() : "");
            row.createCell(4).setCellValue(dto.getDatePeremption() != null ? dto.getDatePeremption() : "");
            row.createCell(5).setCellValue(dto.getQuantity());
            row.createCell(6).setCellValue(dto.getPrixAchat() / 100.0);
            row.createCell(7).setCellValue(dto.getPrixVente() / 100.0);
            row.createCell(8).setCellValue(dto.getPrixTotalVente() / 100.0);
            row.createCell(9).setCellValue(dto.getPrixTotaAchat() / 100.0);
            row.createCell(10).setCellValue(dto.getStatutPerime() != null ? dto.getStatutPerime() : "");
            row.createCell(11).setCellValue(dto.getRayonName() != null ? dto.getRayonName() : "");
            row.createCell(12).setCellValue(dto.getFamilleProduitName() != null ? dto.getFamilleProduitName() : "");
        });
    }

    public byte[] exportToCsv(LotFilterParam lotFilterParam) throws IOException {
        List<LotPerimeDTO> data = lotService.findLotsPerimes(lotFilterParam, Pageable.unpaged()).getContent();
        String title = buildReportTitle(lotFilterParam.getFromDate(), lotFilterParam.getToDate());

        byte[] csvData = csvExportService.createCsvReport(title, HEADERS, data, dto -> new String[] {
            dto.getNumLot() != null ? dto.getNumLot() : "",
            dto.getFournisseur() != null ? dto.getFournisseur() : "",
            dto.getProduitName() != null ? dto.getProduitName() : "",
            dto.getProduitCode() != null ? dto.getProduitCode() : "",
            dto.getDatePeremption() != null ? dto.getDatePeremption() : "",
            String.valueOf(dto.getQuantity()),
            String.format("%.2f", dto.getPrixAchat() / 100.0),
            String.format("%.2f", dto.getPrixVente() / 100.0),
            String.format("%.2f", dto.getPrixTotalVente() / 100.0),
            String.format("%.2f", dto.getPrixTotaAchat() / 100.0),
            dto.getStatutPerime() != null ? dto.getStatutPerime() : "",
            dto.getRayonName() != null ? dto.getRayonName() : "",
            dto.getFamilleProduitName() != null ? dto.getFamilleProduitName() : ""
        });

        return csvExportService.addUtf8Bom(csvData);
    }

    private String buildReportTitle(LocalDate fromDate, LocalDate toDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        StringBuilder title = new StringBuilder("Liste des produits périmés");
        if (fromDate != null && toDate != null) {
            title.append(" du ").append(fromDate.format(formatter)).append(" au ").append(toDate.format(formatter));
        } else if (fromDate != null) {
            title.append(" à partir du ").append(fromDate.format(formatter));
        } else if (toDate != null) {
            title.append(" jusqu'au ").append(toDate.format(formatter));
        }
        return title.toString();
    }
}
