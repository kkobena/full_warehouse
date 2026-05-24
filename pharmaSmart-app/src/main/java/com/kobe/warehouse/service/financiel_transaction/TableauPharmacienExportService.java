package com.kobe.warehouse.service.financiel_transaction;

import static com.kobe.warehouse.service.financiel_transaction.TableauPharmacienConstants.*;

import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import com.kobe.warehouse.service.report.excel.ReportExcelExportService;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Handles Excel export for TableauPharmacien
 */
@Service
public class TableauPharmacienExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ReportExcelExportService excelExportService;

    public TableauPharmacienExportService(ReportExcelExportService excelExportService) {
        this.excelExportService = excelExportService;
    }

    /**
     * Export tableau to Excel format as byte array
     */
    public byte[] exportToExcel(TableauPharmacienWrapper wrapper, List<GroupeFournisseurDTO> supplierGroups) {
        String[] headers = buildColumnHeaders(supplierGroups);
        List<TableauPharmacienDTO> data = wrapper.getTableauPharmaciens();

        try {
            return excelExportService.createExcelReport(EXCEL_SHEET_NAME, headers, data, (row, dto) -> {
                int cellIndex = 0;
                // Date
                row.createCell(cellIndex++).setCellValue(dto.getMvtDate() != null ? dto.getMvtDate().format(DATE_FORMATTER) : "");
                // Sales data
                row.createCell(cellIndex++).setCellValue(dto.getMontantComptant());
                row.createCell(cellIndex++).setCellValue(dto.getMontantCredit());
                row.createCell(cellIndex++).setCellValue(dto.getMontantRemise());
                row.createCell(cellIndex++).setCellValue(dto.getMontantNet());
                row.createCell(cellIndex++).setCellValue(dto.getNombreVente());

                // Supplier group amounts
                for (GroupeFournisseurDTO group : supplierGroups) {
                    long amount = dto
                        .getGroupAchats()
                        .stream()
                        .filter(f -> Objects.equals(f.getId(), group.getId()))
                        .mapToLong(f -> f.getAchat().getMontantNet())
                        .sum();
                    row.createCell(cellIndex++).setCellValue(amount);
                }

                // Purchase data and ratios
                row.createCell(cellIndex++).setCellValue(dto.getMontantAvoirFournisseur());
                row.createCell(cellIndex++).setCellValue(dto.getMontantBonAchat());
                row.createCell(cellIndex++).setCellValue(dto.getRatioVenteAchat());
                row.createCell(cellIndex).setCellValue(dto.getRatioAchatVente());
            });
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du fichier Excel", e);
        }
    }

    /**
     * Build column headers
     */
    private String[] buildColumnHeaders(List<GroupeFournisseurDTO> supplierGroups) {
        List<String> headers = new ArrayList<>(
            List.of(COL_DATE, COL_COMPTANT, COL_CREDIT, COL_REMISE, COL_MONTANT_NET, COL_NOMBRE_CLIENTS)
        );

        // Add supplier group columns
        supplierGroups.forEach(group -> headers.add(group.getLibelle()));

        // Add remaining columns
        headers.addAll(List.of(COL_AVOIRS, COL_ACHATS_NETS, COL_RATIO_VA, COL_RATIO_AV));

        return headers.toArray(new String[0]);
    }
}
