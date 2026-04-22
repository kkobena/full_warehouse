package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.AvoirTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.AvoirStatut;
import com.kobe.warehouse.repository.AvoirTiersPayantRepository;
import com.kobe.warehouse.service.facturation.dto.AvoirSearchParams;
import com.kobe.warehouse.service.report.excel.ReportExcelExportService;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional(readOnly = true)
public class AvoirExcelService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] HEADERS = {
        "N° Avoir", "Facture origine", "Date avoir", "Tiers-payant",
        "Montant TTC", "TVA", "HT", "Statut", "Motif"
    };

    private final AvoirTiersPayantRepository avoirRepository;
    private final ReportExcelExportService excelService;

    public AvoirExcelService(
        AvoirTiersPayantRepository avoirRepository,
        ReportExcelExportService excelService
    ) {
        this.avoirRepository = avoirRepository;
        this.excelService = excelService;
    }

    public byte[] exportToExcel(AvoirSearchParams params) throws IOException {
        List<AvoirStatut> statuts = !CollectionUtils.isEmpty(params.statuts())
            ? params.statuts()
            : List.of(AvoirStatut.values());
        LocalDate start = params.startDate() != null ? params.startDate() : LocalDate.now().minusMonths(6);
        LocalDate end = params.endDate() != null ? params.endDate() : LocalDate.now();
        String numAvoir = (params.numAvoir() != null && !params.numAvoir().isBlank()) ? "%" + params.numAvoir().toLowerCase() + "%" : null;

        List<AvoirTiersPayant> avoirs;
        if (params.tiersPayantId() != null) {
            avoirs = avoirRepository.searchByTiersPayant(params.tiersPayantId(), start, end, statuts, numAvoir, Pageable.unpaged()).getContent();
        } else {
            avoirs = avoirRepository.searchAll(start, end, statuts, numAvoir, Pageable.unpaged()).getContent();
        }

        String title = "Avoirs / Notes de crédit — " + start.format(DATE_FMT) + " au " + end.format(DATE_FMT);
        return excelService.createExcelReport(title, HEADERS, avoirs, this::fillRow);
    }

    private void fillRow(Row row, AvoirTiersPayant avoir) {
        FactureTiersPayant facture = avoir.getFactureTiersPayant();
        TiersPayant tp = facture.getTiersPayant();
        GroupeTiersPayant gtp = Objects.isNull(tp) ? facture.getGroupeTiersPayant() : null;
        String tpName = tp != null ? tp.getFullName() : (gtp != null ? gtp.getName() : "—");

        row.createCell(0).setCellValue(avoir.getNumAvoir());
        row.createCell(1).setCellValue(facture.getNumFacture());
        row.createCell(2).setCellValue(avoir.getAvoirDate() != null ? avoir.getAvoirDate().format(DATE_FMT) : "");
        row.createCell(3).setCellValue(tpName);
        row.createCell(4).setCellValue(avoir.getMontantAvoir() != null ? avoir.getMontantAvoir().doubleValue() : 0);
        row.createCell(5).setCellValue(avoir.getMontantTva() != null ? avoir.getMontantTva().doubleValue() : 0);
        row.createCell(6).setCellValue(avoir.getMontantHt() != null ? avoir.getMontantHt().doubleValue() : 0);
        row.createCell(7).setCellValue(avoir.getStatut() != null ? avoir.getStatut().name() : "");
        row.createCell(8).setCellValue(avoir.getMotif() != null ? avoir.getMotif() : "");
    }
}
