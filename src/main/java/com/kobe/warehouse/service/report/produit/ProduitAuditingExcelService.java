package com.kobe.warehouse.service.report.produit;

import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.report.excel.ReportExcelExportService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ProduitAuditingExcelService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] HEADERS = {
        "Date", "Qté init",
        "Vente", "Ret. fourn.", "Périmée", "Ajust.−", "Décon.−", "Transf.↓",
        "Entrée", "Ajust.+", "Décon.+", "Annulée", "Transf.↑",
        "Qté inv.", "Écart",
        "Stock final"
    };

    private final ReportExcelExportService excelExportService;

    public ProduitAuditingExcelService(ReportExcelExportService excelExportService) {
        this.excelExportService = excelExportService;
    }

    /**
     * Génère le fichier XLSX et retourne une Resource Spring
     */
    public byte[] exportToExcel(List<ProduitAuditingState> items, String title) throws IOException {

        return excelExportService.createExcelReport(title, HEADERS, items, this::fillRow);


    }

    private void fillRow(Row row, ProduitAuditingState s) {
        row.createCell(0).setCellValue(s.getMvtDate() != null ? s.getMvtDate().format(DATE_FMT) : "");
        setNum(row, 1, s.getInitStock(), false);
        setNum(row, 2, s.getSaleQuantity(), true);
        setNum(row, 3, s.getRetourFournisseurQuantity(), true);
        setNum(row, 4, s.getPerimeQuantity(), true);
        setNum(row, 5, s.getAjustementNegatifQuantity(), true);
        setNum(row, 6, s.getDeconNegatifQuantity(), true);
        setNum(row, 7, s.getMouvementStockOut(), true);
        setNum(row, 8, s.getDeleveryQuantity(), true);
        setNum(row, 9, s.getAjustementPositifQuantity(), true);
        setNum(row, 10, s.getDeconPositifQuantity(), true);
        setNum(row, 11, s.getCanceledQuantity(), true);
        setNum(row, 12, s.getMouvementStockIn(), true);
        setNum(row, 13, s.getStoreInventoryQuantity(), true);
        setNum(row, 14, s.getInventoryGap(), true);
        setNum(row, 15, s.getAfterStock(), false);
    }

    private void setNum(Row row, int col, int value, boolean dashIfZero) {
        Cell cell = row.createCell(col);
        if (dashIfZero && value == 0) {
            cell.setCellValue("—");
        } else {
            cell.setCellValue(value);
        }
    }
}

