package com.kobe.warehouse.service.report.excel;

import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.service.dto.RetourBonDTO;
import com.kobe.warehouse.service.dto.RetourBonItemDTO;
import com.kobe.warehouse.service.stock.RetourBonService;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service d'export Excel/CSV des bons de retour fournisseur.
 * Exporte les lignes de retour (RetourBonItem) avec leurs informations de lot, produit et fournisseur.
 */
@Service
@Transactional(readOnly = true)
public class RetourBonExcelCsvReportService {

    private static final String[] HEADERS = {
        "N° Bon de retour",
        "Date du retour",
        "Fournisseur",
        "Statut",
        "Référence réception",
        "Produit",
        "CIP",
        "N° Lot",
        "Quantité retournée",
        "Quantité acceptée",
        "Prix d'achat (unitaire)",
        "Montant retour",
        "Motif"
    };

    private final ReportExcelExportService excelExportService;
    private final CsvExportService csvExportService;
    private final RetourBonService retourBonService;

    public RetourBonExcelCsvReportService(
        ReportExcelExportService excelExportService,
        CsvExportService csvExportService,
        RetourBonService retourBonService
    ) {
        this.excelExportService = excelExportService;
        this.csvExportService = csvExportService;
        this.retourBonService = retourBonService;
    }

    public byte[] exportToExcel(RetourStatut statut, LocalDate dtStart, LocalDate dtEnd, String search)
        throws IOException {
        List<RetourBonItemLine> lines = buildLines(statut, dtStart, dtEnd, search);
        String title = buildReportTitle(dtStart, dtEnd);

        return excelExportService.createExcelReport(title, HEADERS, lines, (row, line) -> {
            row.createCell(0).setCellValue(line.retourBonId());
            row.createCell(1).setCellValue(line.dateMtv() != null ? line.dateMtv() : "");
            row.createCell(2).setCellValue(line.fournisseurLibelle() != null ? line.fournisseurLibelle() : "");
            row.createCell(3).setCellValue(line.statut() != null ? line.statut() : "");
            row.createCell(4).setCellValue(line.receiptReference() != null ? line.receiptReference() : "");
            row.createCell(5).setCellValue(line.produitLibelle() != null ? line.produitLibelle() : "");
            row.createCell(6).setCellValue(line.produitCip() != null ? line.produitCip() : "");
            row.createCell(7).setCellValue(line.lotNumero() != null ? line.lotNumero() : "");
            row.createCell(8).setCellValue(line.qtyMvt());
            row.createCell(9).setCellValue(line.acceptedQty() != null ? line.acceptedQty() : 0);
            row.createCell(10).setCellValue(line.prixAchat() != null ? line.prixAchat() : 0);
            row.createCell(11).setCellValue(line.montantRetour() != null ? line.montantRetour() : 0);
            row.createCell(12).setCellValue(line.motifLibelle() != null ? line.motifLibelle() : "");
        });
    }

    public byte[] exportToCsv(RetourStatut statut, LocalDate dtStart, LocalDate dtEnd, String search)
        throws IOException {
        List<RetourBonItemLine> lines = buildLines(statut, dtStart, dtEnd, search);
        String title = buildReportTitle(dtStart, dtEnd);

        byte[] csv = csvExportService.createCsvReport(title, HEADERS, lines, line -> new String[] {
            String.valueOf(line.retourBonId()),
            line.dateMtv() != null ? line.dateMtv() : "",
            line.fournisseurLibelle() != null ? line.fournisseurLibelle() : "",
            line.statut() != null ? line.statut() : "",
            line.receiptReference() != null ? line.receiptReference() : "",
            line.produitLibelle() != null ? line.produitLibelle() : "",
            line.produitCip() != null ? line.produitCip() : "",
            line.lotNumero() != null ? line.lotNumero() : "",
            String.valueOf(line.qtyMvt()),
            String.valueOf(line.acceptedQty() != null ? line.acceptedQty() : 0),
            String.valueOf(line.prixAchat() != null ? line.prixAchat() : 0),
            String.valueOf(line.montantRetour() != null ? line.montantRetour() : 0),
            line.motifLibelle() != null ? line.motifLibelle() : ""
        });

        return csvExportService.addUtf8Bom(csv);
    }

    private List<RetourBonItemLine> buildLines(RetourStatut statut, LocalDate dtStart, LocalDate dtEnd, String search) {
        List<RetourBonDTO> bons = retourBonService
            .findAll(statut, dtStart, dtEnd, search, Pageable.unpaged())
            .getContent();

        List<RetourBonItemLine> lines = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (RetourBonDTO bon : bons) {
            for (RetourBonItemDTO item : bon.getRetourBonItems()) {
                int montant = item.getPrixAchat() != null ? item.getPrixAchat() * item.getQtyMvt() : 0;
                lines.add(new RetourBonItemLine(
                    bon.getId(),
                    bon.getDateMtv() != null ? bon.getDateMtv().format(fmt) : "",
                    bon.getFournisseurLibelle(),
                    bon.getStatut() != null ? bon.getStatut().name() : "",
                    bon.getReceiptReference(),
                    item.getProduitLibelle(),
                    item.getProduitCip(),
                    item.getLotNumero(),
                    item.getQtyMvt() != null ? item.getQtyMvt() : 0,
                    item.getAcceptedQty(),
                    item.getPrixAchat(),
                    montant,
                    item.getMotifRetourLibelle()
                ));
            }
        }
        return lines;
    }

    private String buildReportTitle(LocalDate dtStart, LocalDate dtEnd) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        StringBuilder title = new StringBuilder("Liste des retours fournisseur");
        if (dtStart != null && dtEnd != null) {
            title.append(" du ").append(dtStart.format(formatter)).append(" au ").append(dtEnd.format(formatter));
        } else if (dtStart != null) {
            title.append(" à partir du ").append(dtStart.format(formatter));
        } else if (dtEnd != null) {
            title.append(" jusqu'au ").append(dtEnd.format(formatter));
        }
        return title.toString();
    }

    /**
     * Record représentant une ligne du rapport (une ligne de RetourBonItem aplatie).
     */
    record RetourBonItemLine(
        Integer retourBonId,
        String dateMtv,
        String fournisseurLibelle,
        String statut,
        String receiptReference,
        String produitLibelle,
        String produitCip,
        String lotNumero,
        int qtyMvt,
        Integer acceptedQty,
        Integer prixAchat,
        Integer montantRetour,
        String motifLibelle
    ) {}
}

