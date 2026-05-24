package com.kobe.warehouse.service.report.excel;

import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyDTO;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyFilter;
import com.kobe.warehouse.service.product_to_destroy.service.ProductsToDestroyService;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductToDestroyExcelCsvReportService {

    private static final String[] HEADERS = {
        "Nom du produit",
        "Code CIP",
        "Lot",
        "Quantité",
        "Date de péremption",
        "Date de destruction",
        "Utilisateur",
        "Date de création",
        "Date de modification",
        "Fournisseur",
        "Prix d'achat",
        "Prix de vente",
        "Stock déjà détruit"
    };

    private final ReportExcelExportService excelExportService;
    private final CsvExportService csvExportService;
    private final ProductsToDestroyService productsToDestroyService;

    public ProductToDestroyExcelCsvReportService(
        ReportExcelExportService excelExportService,
        CsvExportService csvExportService,
        ProductsToDestroyService productsToDestroyService
    ) {
        this.excelExportService = excelExportService;
        this.csvExportService = csvExportService;
        this.productsToDestroyService = productsToDestroyService;
    }

    public byte[] exportToExcel(ProductToDestroyFilter filter) throws IOException {
        List<ProductToDestroyDTO> data = productsToDestroyService.findAll(filter, Pageable.unpaged()).getContent();
        String title = buildReportTitle(filter.fromDate(), filter.toDate());

        return excelExportService.createExcelReport(title, HEADERS, data, (row, dto) -> {
            row.createCell(0).setCellValue(dto.getProduitName() != null ? dto.getProduitName() : "");
            row.createCell(1).setCellValue(dto.getProduitCodeCip() != null ? dto.getProduitCodeCip() : "");
            row.createCell(2).setCellValue(dto.getNumLot() != null ? dto.getNumLot() : "");
            row.createCell(3).setCellValue(dto.getQuantity());
            row.createCell(4).setCellValue(dto.getDatePeremption() != null ? dto.getDatePeremption() : "");
            row.createCell(5).setCellValue(dto.getDateDestruction() != null ? dto.getDateDestruction() : "");
            row.createCell(6).setCellValue(dto.getUser() != null ? dto.getUser() : "");
            row.createCell(7).setCellValue(dto.getCreatedDate() != null ? dto.getCreatedDate() : "");
            row.createCell(8).setCellValue(dto.getUpdatedDate() != null ? dto.getUpdatedDate() : "");
            row.createCell(9).setCellValue(dto.getFournisseur() != null ? dto.getFournisseur() : "");
            row.createCell(10).setCellValue(dto.getPrixAchat() / 100.0);
            row.createCell(11).setCellValue(dto.getPrixUni() / 100.0);
            row.createCell(12).setCellValue(dto.isDestroyed() ? "Oui" : "Non");
        });
    }

    public byte[] exportToCsv(ProductToDestroyFilter filter) throws IOException {
        List<ProductToDestroyDTO> data = productsToDestroyService.findAll(filter, Pageable.unpaged()).getContent();
        String title = buildReportTitle(filter.fromDate(), filter.toDate());

        byte[] csvData = csvExportService.createCsvReport(title, HEADERS, data, dto -> new String[] {
            dto.getProduitName() != null ? dto.getProduitName() : "",
            dto.getProduitCodeCip() != null ? dto.getProduitCodeCip() : "",
            dto.getNumLot() != null ? dto.getNumLot() : "",
            String.valueOf(dto.getQuantity()),
            dto.getDatePeremption() != null ? dto.getDatePeremption() : "",
            dto.getDateDestruction() != null ? dto.getDateDestruction() : "",
            dto.getUser() != null ? dto.getUser() : "",
            dto.getCreatedDate() != null ? dto.getCreatedDate() : "",
            dto.getUpdatedDate() != null ? dto.getUpdatedDate() : "",
            dto.getFournisseur() != null ? dto.getFournisseur() : "",
            String.format("%.2f", dto.getPrixAchat() / 100.0),
            String.format("%.2f", dto.getPrixUni() / 100.0),
            dto.isDestroyed() ? "Oui" : "Non"
        });

        return csvExportService.addUtf8Bom(csvData);
    }

    private String buildReportTitle(LocalDate fromDate, LocalDate toDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        StringBuilder title = new StringBuilder("Liste des produits à détruire");
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
