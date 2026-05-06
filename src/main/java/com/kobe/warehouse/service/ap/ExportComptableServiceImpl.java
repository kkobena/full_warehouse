package com.kobe.warehouse.service.ap;

import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.dto.filter.TransactionFilterDTO;
import com.kobe.warehouse.service.dto.projection.GroupeFournisseurAchat;
import com.kobe.warehouse.service.financiel_transaction.FinancialTransactionService;
import com.kobe.warehouse.service.financiel_transaction.TaxeService;
import com.kobe.warehouse.service.financiel_transaction.TvaReportReportService;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeWrapperDTO;
import com.kobe.warehouse.service.report.excel.CsvExportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExportComptableServiceImpl implements ExportComptableService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int ROW_WINDOW = 100;

    private final TaxeService taxeService;
    private final CommandeRepository commandeRepository;
    private final FinancialTransactionService transactionService;
    private final CsvExportService csvExportService;
    private final TvaReportReportService tvaReportService;

    @PersistenceContext
    private EntityManager em;

    public ExportComptableServiceImpl(
        TaxeService taxeService,
        CommandeRepository commandeRepository,
        FinancialTransactionService transactionService,
        CsvExportService csvExportService,
        TvaReportReportService tvaReportService
    ) {
        this.taxeService = taxeService;
        this.commandeRepository = commandeRepository;
        this.transactionService = transactionService;
        this.csvExportService = csvExportService;
        this.tvaReportService = tvaReportService;
    }

    @Override
    public byte[] export(
        LocalDate startDate,
        LocalDate endDate,
        String format,
        boolean ventes,
        boolean achats,
        boolean mvtCaisse,
        boolean tiersPayant,
        boolean differes,
        boolean tva
    ) throws IOException {
        return switch (format) {
            case "excel" -> buildExcel(startDate, endDate, ventes, achats, mvtCaisse, tiersPayant, differes, tva);
            case "csv" -> buildCsv(startDate, endDate, ventes, achats, mvtCaisse, tiersPayant, differes, tva);
            case "pdf" -> buildPdf(startDate, endDate);
            default -> throw new IllegalArgumentException("Format non supporté: " + format);
        };
    }

    // ─── PDF ────────────────────────────────────────────────────────────────────

    private byte[] buildPdf(LocalDate startDate, LocalDate endDate) {
        MvtParam param = buildParam(startDate, endDate);

        return tvaReportService.exportToPdfBytes(taxeService.fetchDeclarationTva(param), new ReportPeriode(startDate, endDate), false);
    }

    // ─── EXCEL ──────────────────────────────────────────────────────────────────

    private byte[] buildExcel(
        LocalDate start,
        LocalDate end,
        boolean ventes,
        boolean achats,
        boolean mvtCaisse,
        boolean tiersPayant,
        boolean differes,
        boolean tva
    ) throws IOException {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(ROW_WINDOW);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            MvtParam param = buildParam(start, end);

            if (ventes) {
                TaxeWrapperDTO data = taxeService.fetchTaxe(param, true);
                if (data != null) addVentesSheet(wb, data, start, end);
            }
            if (achats) {
                List<GroupeFournisseurAchat> data = commandeRepository
                    .fetchAchats(start, end, OrderStatut.CLOSED, Pageable.unpaged())
                    .getContent();
                addAchatsSheet(wb, data, start, end);
            }
            if (mvtCaisse) {
                List<MvtCaisseDTO> data = transactionService
                    .findAll(new TransactionFilterDTO(start, end, null, null, null, null, null, null, null, null), Pageable.unpaged())
                    .getContent();
                addMvtCaisseSheet(wb, data, start, end);
            }
            if (tiersPayant) {
                addTiersPayantSheet(wb, fetchTiersPayantRows(start, end), start, end);
            }
            if (differes) {
                addDifferesSheet(wb, fetchDifferesRows(), start, end);
            }
            if (tva) {
                TaxeWrapperDTO data = taxeService.fetchDeclarationTva(param);
                if (data != null) addTvaSheet(wb, data, start, end);
            }

            if (wb.getNumberOfSheets() == 0) {
                wb.createSheet("Export vide");
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    private void addVentesSheet(SXSSFWorkbook wb, TaxeWrapperDTO data, LocalDate from, LocalDate to) {
        String[] headers = { "Taux TVA (%)", "CA HT (FCFA)", "TVA collectée (FCFA)", "CA TTC (FCFA)", "Montant net (FCFA)", "Remises (FCFA)" };
        SXSSFSheet sheet = createSheet(wb, "Ventes");
        int row = writeHeader(sheet, wb, "Ventes — " + period(from, to), headers);

        for (TaxeDTO t : data.getTaxes()) {
            Row r = sheet.createRow(row++);
            r.createCell(0).setCellValue(t.getCodeTva() != null ? t.getCodeTva() + "%" : "");
            r.createCell(1).setCellValue(orZero(t.getMontantHt()));
            r.createCell(2).setCellValue(orZero(t.getMontantTaxe()));
            r.createCell(3).setCellValue(orZero(t.getMontantTtc()));
            r.createCell(4).setCellValue(orZero(t.getMontantNet()));
            r.createCell(5).setCellValue(t.getMontantRemise());
        }
        Row total = sheet.createRow(row);
        applyBold(wb, total.createCell(0)).setCellValue("TOTAL");
        total.createCell(1).setCellValue(data.getMontantHt());
        total.createCell(2).setCellValue(data.getMontantTaxe());
        total.createCell(3).setCellValue(data.getMontantTtc());
        total.createCell(4).setCellValue(data.getMontantNet());
        total.createCell(5).setCellValue(data.getMontantRemise());
        autoSize(sheet, headers.length);
    }

    private void addAchatsSheet(SXSSFWorkbook wb, List<GroupeFournisseurAchat> data, LocalDate from, LocalDate to) {
        String[] headers = { "Groupe fournisseur", "Montant HT (FCFA)", "TVA (FCFA)", "Montant TTC (FCFA)" };
        SXSSFSheet sheet = createSheet(wb, "Achats");
        int row = writeHeader(sheet, wb, "Achats — " + period(from, to), headers);

        long sumHt = 0, sumTva = 0, sumTtc = 0;
        for (GroupeFournisseurAchat a : data) {
            Row r = sheet.createRow(row++);
            r.createCell(0).setCellValue(a.getLibelle() != null ? a.getLibelle() : "(sans groupe)");
            long ht = toLong(a.getMontantHt());
            long tva = toLong(a.getMontantTva());
            long ttc = toLong(a.getMontantTtc());
            r.createCell(1).setCellValue(ht);
            r.createCell(2).setCellValue(tva);
            r.createCell(3).setCellValue(ttc);
            sumHt += ht;
            sumTva += tva;
            sumTtc += ttc;
        }
        Row total = sheet.createRow(row);
        applyBold(wb, total.createCell(0)).setCellValue("TOTAL");
        total.createCell(1).setCellValue(sumHt);
        total.createCell(2).setCellValue(sumTva);
        total.createCell(3).setCellValue(sumTtc);
        autoSize(sheet, headers.length);
    }

    private void addMvtCaisseSheet(SXSSFWorkbook wb, List<MvtCaisseDTO> data, LocalDate from, LocalDate to) {
        String[] headers = { "Date", "Type", "N° Bon", "Mode paiement", "Montant (FCFA)" };
        SXSSFSheet sheet = createSheet(wb, "Mvt caisse");
        int row = writeHeader(sheet, wb, "Mouvements de caisse — " + period(from, to), headers);

        long sumMontant = 0;
        for (MvtCaisseDTO m : data) {
            Row r = sheet.createRow(row++);
            r.createCell(0).setCellValue(m.getTransactionDate() != null ? m.getTransactionDate() : m.getDate() != null ? m.getDate() : "");
            r.createCell(1).setCellValue(m.getTransactionType() != null ? m.getTransactionType() : "");
            r.createCell(2).setCellValue(m.getNumBon() != null ? m.getNumBon() : m.getReference() != null ? m.getReference() : "");
            r.createCell(3).setCellValue(m.getPaymentModeLibelle() != null ? m.getPaymentModeLibelle() : "");
            r.createCell(4).setCellValue(m.getMontant());
            sumMontant += m.getMontant();
        }
        Row total = sheet.createRow(row);
        applyBold(wb, total.createCell(0)).setCellValue("TOTAL");
        total.createCell(4).setCellValue(sumMontant);
        autoSize(sheet, headers.length);
    }

    private void addTiersPayantSheet(SXSSFWorkbook wb, List<Object[]> data, LocalDate from, LocalDate to) {
        String[] headers = { "Tiers payant", "Montant dû (FCFA)", "Montant réglé (FCFA)", "Solde (FCFA)" };
        SXSSFSheet sheet = createSheet(wb, "Tiers payant");
        int row = writeHeader(sheet, wb, "Tiers payant — " + period(from, to), headers);

        long sumDu = 0, sumRegle = 0, sumSolde = 0;
        for (Object[] r : data) {
            Row sheetRow = sheet.createRow(row++);
            String name = r[0] != null ? r[0].toString() : "";
            long montant = toLong(r[1]);
            long regle = toLong(r[2]);
            long solde = montant - regle;
            sheetRow.createCell(0).setCellValue(name);
            sheetRow.createCell(1).setCellValue(montant);
            sheetRow.createCell(2).setCellValue(regle);
            sheetRow.createCell(3).setCellValue(solde);
            sumDu += montant;
            sumRegle += regle;
            sumSolde += solde;
        }
        Row total = sheet.createRow(row);
        applyBold(wb, total.createCell(0)).setCellValue("TOTAL");
        total.createCell(1).setCellValue(sumDu);
        total.createCell(2).setCellValue(sumRegle);
        total.createCell(3).setCellValue(sumSolde);
        autoSize(sheet, headers.length);
    }

    private void addDifferesSheet(SXSSFWorkbook wb, List<Object[]> data, LocalDate from, LocalDate to) {
        String[] headers = { "Tiers payant", "Montant total (FCFA)", "Réglé (FCFA)", "Solde en attente (FCFA)" };
        SXSSFSheet sheet = createSheet(wb, "Différés");
        int row = writeHeader(sheet, wb, "Créances différées (toutes périodes)", headers);

        long sumTotal = 0, sumRegle = 0, sumSolde = 0;
        for (Object[] r : data) {
            Row sheetRow = sheet.createRow(row++);
            String name = r[0] != null ? r[0].toString() : "";
            long total = toLong(r[1]);
            long regle = toLong(r[2]);
            long solde = total - regle;
            sheetRow.createCell(0).setCellValue(name);
            sheetRow.createCell(1).setCellValue(total);
            sheetRow.createCell(2).setCellValue(regle);
            sheetRow.createCell(3).setCellValue(solde);
            sumTotal += total;
            sumRegle += regle;
            sumSolde += solde;
        }
        Row totalRow = sheet.createRow(row);
        applyBold(wb, totalRow.createCell(0)).setCellValue("TOTAL");
        totalRow.createCell(1).setCellValue(sumTotal);
        totalRow.createCell(2).setCellValue(sumRegle);
        totalRow.createCell(3).setCellValue(sumSolde);
        autoSize(sheet, headers.length);
    }

    private void addTvaSheet(SXSSFWorkbook wb, TaxeWrapperDTO data, LocalDate from, LocalDate to) {
        String[] headers = { "Taux TVA (%)", "Base HT ventes", "TVA collectée", "Base HT achats", "TVA déductible", "TVA nette" };
        SXSSFSheet sheet = createSheet(wb, "TVA");
        int row = writeHeader(sheet, wb, "Déclaration TVA — " + period(from, to), headers);

        for (TaxeDTO t : data.getTaxes()) {
            long achatHt = orZero(t.getMontantAchat());
            int taux = t.getCodeTva() != null ? t.getCodeTva() : 0;
            long tvaDeductible = Math.round(achatHt * taux / 100.0);
            long tvaCollectee = orZero(t.getMontantTaxe());
            Row r = sheet.createRow(row++);
            r.createCell(0).setCellValue(taux + "%");
            r.createCell(1).setCellValue(orZero(t.getMontantHt()));
            r.createCell(2).setCellValue(tvaCollectee);
            r.createCell(3).setCellValue(achatHt);
            r.createCell(4).setCellValue(tvaDeductible);
            r.createCell(5).setCellValue(tvaCollectee - tvaDeductible);
        }
        Row total = sheet.createRow(row++);
        applyBold(wb, total.createCell(0)).setCellValue("TOTAL");
        total.createCell(1).setCellValue(data.getMontantHt());
        total.createCell(2).setCellValue(data.getMontantTaxe());
        total.createCell(3).setCellValue(data.getMontantAchat());
        total.createCell(4).setCellValue(data.getTvaDeductible());
        total.createCell(5).setCellValue(data.getTvaNette());

        // Résumé net
        Row blankRow = sheet.createRow(row++);
        blankRow.createCell(0).setCellValue("");
        Row sumRow = sheet.createRow(row);
        applyBold(wb, sumRow.createCell(0)).setCellValue("TVA nette à déclarer");
        sumRow.createCell(5).setCellValue(data.getTvaNette());
        autoSize(sheet, headers.length);
    }

    // ─── CSV ────────────────────────────────────────────────────────────────────

    private byte[] buildCsv(
        LocalDate start,
        LocalDate end,
        boolean ventes,
        boolean achats,
        boolean mvtCaisse,
        boolean tiersPayant,
        boolean differes,
        boolean tva
    ) throws IOException {
        MvtParam param = buildParam(start, end);
        List<byte[]> sections = new ArrayList<>();

        if (ventes) {
            TaxeWrapperDTO data = taxeService.fetchTaxe(param, true);
            if (data != null) sections.add(buildVentesCsv(data, start, end));
        }
        if (achats) {
            List<GroupeFournisseurAchat> data = commandeRepository
                .fetchAchats(start, end, OrderStatut.CLOSED, Pageable.unpaged())
                .getContent();
            sections.add(buildAchatsCsv(data, start, end));
        }
        if (mvtCaisse) {
            List<MvtCaisseDTO> data = transactionService
                .findAll(new TransactionFilterDTO(start, end, null, null, null, null, null, null, null, null), Pageable.unpaged())
                .getContent();
            sections.add(buildMvtCaisseCsv(data, start, end));
        }
        if (tiersPayant) {
            sections.add(buildTiersPayantCsv(fetchTiersPayantRows(start, end), start, end));
        }
        if (differes) {
            sections.add(buildDifferesCsv(fetchDifferesRows(), start, end));
        }
        if (tva) {
            TaxeWrapperDTO data = taxeService.fetchDeclarationTva(param);
            if (data != null) sections.add(buildTvaCsv(data, start, end));
        }

        byte[] separator = "\r\n\r\n".getBytes();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < sections.size(); i++) {
            if (i > 0) out.write(separator);
            out.write(sections.get(i));
        }
        return csvExportService.addUtf8Bom(out.toByteArray());
    }

    private byte[] buildVentesCsv(TaxeWrapperDTO data, LocalDate from, LocalDate to) throws IOException {
        String[] headers = { "Taux TVA (%)", "CA HT", "TVA collectée", "CA TTC", "Montant net", "Remises" };
        List<TaxeDTO> lines = data.getTaxes();
        return csvExportService.createCsvReport("Ventes — " + period(from, to), headers, lines, t -> new String[]{
            t.getCodeTva() != null ? t.getCodeTva() + "%" : "",
            String.valueOf(orZero(t.getMontantHt())),
            String.valueOf(orZero(t.getMontantTaxe())),
            String.valueOf(orZero(t.getMontantTtc())),
            String.valueOf(orZero(t.getMontantNet())),
            String.valueOf(t.getMontantRemise())
        });
    }

    private byte[] buildAchatsCsv(List<GroupeFournisseurAchat> data, LocalDate from, LocalDate to) throws IOException {
        String[] headers = { "Groupe fournisseur", "Montant HT", "TVA", "Montant TTC" };
        return csvExportService.createCsvReport("Achats — " + period(from, to), headers, data, a -> new String[]{
            a.getLibelle() != null ? a.getLibelle() : "(sans groupe)",
            String.valueOf(toLong(a.getMontantHt())),
            String.valueOf(toLong(a.getMontantTva())),
            String.valueOf(toLong(a.getMontantTtc()))
        });
    }

    private byte[] buildMvtCaisseCsv(List<MvtCaisseDTO> data, LocalDate from, LocalDate to) throws IOException {
        String[] headers = { "Date", "Type", "N° Bon", "Mode paiement", "Montant" };
        return csvExportService.createCsvReport("Mouvements de caisse — " + period(from, to), headers, data, m -> new String[]{
            m.getTransactionDate() != null ? m.getTransactionDate() : m.getDate() != null ? m.getDate() : "",
            m.getTransactionType() != null ? m.getTransactionType() : "",
            m.getNumBon() != null ? m.getNumBon() : m.getReference() != null ? m.getReference() : "",
            m.getPaymentModeLibelle() != null ? m.getPaymentModeLibelle() : "",
            String.valueOf(m.getMontant())
        });
    }

    private byte[] buildTiersPayantCsv(List<Object[]> data, LocalDate from, LocalDate to) throws IOException {
        String[] headers = { "Tiers payant", "Montant dû", "Montant réglé", "Solde" };
        return csvExportService.createCsvReport("Tiers payant — " + period(from, to), headers, data, r -> new String[]{
            r[0] != null ? r[0].toString() : "",
            String.valueOf(toLong(r[1])),
            String.valueOf(toLong(r[2])),
            String.valueOf(toLong(r[1]) - toLong(r[2]))
        });
    }

    private byte[] buildDifferesCsv(List<Object[]> data, LocalDate from, LocalDate to) throws IOException {
        String[] headers = { "Tiers payant", "Montant total", "Réglé", "Solde en attente" };
        return csvExportService.createCsvReport("Créances différées", headers, data, r -> {
            long total = toLong(r[1]);
            long regle = toLong(r[2]);
            return new String[]{
                r[0] != null ? r[0].toString() : "",
                String.valueOf(total),
                String.valueOf(regle),
                String.valueOf(total - regle)
            };
        });
    }

    private byte[] buildTvaCsv(TaxeWrapperDTO data, LocalDate from, LocalDate to) throws IOException {
        String[] headers = { "Taux TVA (%)", "Base HT ventes", "TVA collectée", "Base HT achats", "TVA déductible", "TVA nette" };
        List<TaxeDTO> lines = data.getTaxes();
        return csvExportService.createCsvReport("Déclaration TVA — " + period(from, to), headers, lines, t -> {
            long achatHt = orZero(t.getMontantAchat());
            int taux = t.getCodeTva() != null ? t.getCodeTva() : 0;
            long tvaDeductible = Math.round(achatHt * taux / 100.0);
            long tvaCollectee = orZero(t.getMontantTaxe());
            return new String[]{
                taux + "%",
                String.valueOf(orZero(t.getMontantHt())),
                String.valueOf(tvaCollectee),
                String.valueOf(achatHt),
                String.valueOf(tvaDeductible),
                String.valueOf(tvaCollectee - tvaDeductible)
            };
        });
    }

    // ─── Requêtes JPQL ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Object[]> fetchTiersPayantRows(LocalDate start, LocalDate end) {
        return em.createQuery(
            "SELECT tp.name, SUM(tpsl.montant), SUM(COALESCE(tpsl.montantRegle, 0)) " +
            "FROM ThirdPartySaleLine tpsl " +
            "JOIN tpsl.clientTiersPayant ctp " +
            "JOIN ctp.tiersPayant tp " +
            "WHERE tpsl.saleDate BETWEEN :start AND :end " +
            "AND tpsl.statut != :deleted " +
            "GROUP BY tp.name " +
            "ORDER BY SUM(tpsl.montant) DESC"
        )
        .setParameter("start", start)
        .setParameter("end", end)
        .setParameter("deleted", ThirdPartySaleStatut.DELETE)
        .getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> fetchDifferesRows() {
        // Returns: [name, SUM(montant), SUM(montantRegle)] — solde computed in caller
        return em.createQuery(
            "SELECT tp.name, SUM(tpsl.montant), SUM(COALESCE(tpsl.montantRegle, 0)) " +
            "FROM ThirdPartySaleLine tpsl " +
            "JOIN tpsl.clientTiersPayant ctp " +
            "JOIN ctp.tiersPayant tp " +
            "WHERE tpsl.statut NOT IN :paidStatuts " +
            "GROUP BY tp.name " +
            "ORDER BY SUM(tpsl.montant) DESC"
        )
        .setParameter("paidStatuts", Set.of(ThirdPartySaleStatut.PAID, ThirdPartySaleStatut.DELETE))
        .getResultList();
    }

    // ─── Helpers Excel ──────────────────────────────────────────────────────────

    private SXSSFSheet createSheet(SXSSFWorkbook wb, String name) {
        SXSSFSheet sheet = wb.createSheet(name);
        sheet.trackAllColumnsForAutoSizing();
        return sheet;
    }

    private int writeHeader(SXSSFSheet sheet, SXSSFWorkbook wb, String title, String[] headers) {
        CellStyle titleStyle = titleStyle(wb);
        CellStyle headerStyle = headerStyle(wb);

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.length - 1));

        Row headerRow = sheet.createRow(1);
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }
        return 2;
    }

    private void autoSize(SXSSFSheet sheet, int cols) {
        for (int i = 0; i < cols; i++) sheet.autoSizeColumn(i);
    }

    private Cell applyBold(SXSSFWorkbook wb, Cell cell) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        cell.setCellStyle(s);
        return cell;
    }

    private CellStyle titleStyle(SXSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        f.setColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private CellStyle headerStyle(SXSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    // ─── Helpers communs ────────────────────────────────────────────────────────

    private MvtParam buildParam(LocalDate start, LocalDate end) {
        return new MvtParam().setFromDate(start).setToDate(end).build();
    }

    private String period(LocalDate from, LocalDate to) {
        return from.format(FMT) + " – " + to.format(FMT);
    }

    private long orZero(Long v) {
        return v != null ? v : 0L;
    }

    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Long l) return l;
        if (o instanceof BigDecimal bd) return bd.longValue();
        if (o instanceof Number n) return n.longValue();
        return 0L;
    }
}
