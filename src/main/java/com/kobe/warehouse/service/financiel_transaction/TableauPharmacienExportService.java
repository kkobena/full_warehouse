package com.kobe.warehouse.service.financiel_transaction;

import static com.kobe.warehouse.service.financiel_transaction.TableauPharmacienConstants.*;

import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import com.kobe.warehouse.service.excel.ExcelExportService;
import com.kobe.warehouse.service.excel.GenericExcelDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Handles Excel export for TableauPharmacien
 */
@Service
public class TableauPharmacienExportService {

    private final ExcelExportService excelExportService;

    public TableauPharmacienExportService(ExcelExportService excelExportService) {
        this.excelExportService = excelExportService;
    }

    /**
     * Export tableau to Excel format
     */
    public org.springframework.core.io.Resource exportToExcel(TableauPharmacienWrapper wrapper, List<GroupeFournisseurDTO> supplierGroups)
        throws IOException {
        GenericExcelDTO excel = buildExcelData(wrapper, supplierGroups);
        return excelExportService.generate(excel, EXCEL_SHEET_NAME, EXCEL_FILE_NAME);
    }

    /**
     * Build Excel data structure
     */
    private GenericExcelDTO buildExcelData(TableauPharmacienWrapper wrapper, List<GroupeFournisseurDTO> supplierGroups) {
        GenericExcelDTO excel = new GenericExcelDTO();

        // Add column headers
        excel.addColumn(buildColumnHeaders(supplierGroups));

        // Add data rows
        wrapper
            .getTableauPharmaciens()
            .forEach(dto -> {
                excel.addRow(buildDataRow(dto, supplierGroups));
            });

        return excel;
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

    /**
     * Build data row for a single TableauPharmacienDTO
     */
    private Object[] buildDataRow(TableauPharmacienDTO dto, List<GroupeFournisseurDTO> supplierGroups) {
        List<Object> row = new ArrayList<>();

        // Sales data
        row.add(dto.getMvtDate());
        row.add(dto.getMontantComptant());
        row.add(dto.getMontantCredit());
        row.add(dto.getMontantRemise());
        row.add(dto.getMontantNet());
        row.add(dto.getNombreVente());

        // Supplier group amounts
        supplierGroups.forEach(group -> {
            long amount = dto
                .getGroupAchats()
                .stream()
                .filter(f -> f.getId() == group.getId())
                .mapToLong(f -> f.getAchat().getMontantNet())
                .sum();
            row.add(amount);
        });

        // Purchase data and ratios
        row.add(dto.getMontantAvoirFournisseur());
        row.add(dto.getMontantBonAchat());
        row.add(dto.getRatioVenteAchat());
        row.add(dto.getRatioAchatVente());

        return row.toArray();
    }
}
