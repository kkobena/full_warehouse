package com.kobe.warehouse.service.report.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * Service générique pour l'export Excel des rapports
 * Utilise Apache POI SXSSFWorkbook pour créer des fichiers .xlsx avec streaming (meilleure gestion mémoire)
 */
@Service
public class ReportExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int ROW_ACCESS_WINDOW_SIZE = 100;

    /**
     * Crée un fichier Excel avec en-tête et données
     *
     * @param title Titre du rapport
     * @param headers En-têtes des colonnes
     * @param data Liste des données
     * @param rowMapper Fonction pour mapper chaque élément de data vers les cellules
     * @param <T> Type des données
     * @return ByteArray du fichier Excel
     */
    public <T> byte[] createExcelReport(String title, String[] headers, List<T> data, BiConsumer<Row, T> rowMapper)
            throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(ROW_ACCESS_WINDOW_SIZE);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            SXSSFSheet sheet = workbook.createSheet(sanitizeSheetName(title));

            // Activer le suivi des colonnes pour autoSizeColumn
            sheet.trackAllColumnsForAutoSizing();

            // Créer les styles
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            int rowNum = 0;

            // Ligne de titre
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(titleStyle);

            // Ligne de date d'export
            Row dateRow = sheet.createRow(rowNum++);
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue("Généré le: " + LocalDateTime.now().format(DATETIME_FORMATTER));
            dateCell.setCellStyle(dateStyle);

            // La fusion n'a de sens qu'à partir de deux colonnes : POI refuse une région d'une seule
            // cellule, et un rapport à colonne unique échouait donc à l'écriture.
            if (headers.length > 1) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.length - 1));
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, headers.length - 1));
            }

            // Ligne vide
            rowNum++;

            // Ligne d'en-têtes
            Row headerRow = sheet.createRow(rowNum++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Lignes de données
            for (T item : data) {
                Row row = sheet.createRow(rowNum++);
                rowMapper.accept(row, item);
            }

            // Auto-dimensionner les colonnes
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Crée un fichier Excel simple avec données en tableau
     */
    public byte[] createSimpleExcelReport(String title, String[] headers, List<String[]> data) throws IOException {
        return createExcelReport(title, headers, data, (row, rowData) -> {
            for (int i = 0; i < rowData.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(rowData[i]);
            }
        });
    }

    /**
     * Un tableau d'une feuille à plusieurs tableaux : son intitulé, ses en-têtes, ses lignes, et une
     * ligne de total facultative, en gras.
     *
     * <p>Une valeur {@link Number} s'écrit en nombre, que le tableur sait additionner ; toute autre
     * valeur, en texte. Une cellule nulle reste vide.
     */
    public record SectionExcel(String titre, String[] entetes, List<Object[]> lignes, Object[] total) {}

    /**
     * Crée un classeur dont chaque feuille enchaîne plusieurs tableaux — une synthèse et ses
     * détails, par exemple. Même présentation que {@link #createExcelReport(String, String[], List,
     * BiConsumer)} : titre, date d'export, en-têtes sur fond bleu.
     *
     * @param title titre du rapport, en tête de chaque feuille
     * @param feuilles les tableaux de chaque feuille, dans l'ordre d'affichage ; la clé nomme la
     *     feuille
     */
    public byte[] createExcelReport(String title, Map<String, List<SectionExcel>> feuilles) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(ROW_ACCESS_WINDOW_SIZE);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle sectionStyle = createSectionStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle entierStyle = createNumberStyle(workbook, "#,##0", false);
            CellStyle decimalStyle = createNumberStyle(workbook, "#,##0.00", false);
            CellStyle totalEntierStyle = createNumberStyle(workbook, "#,##0", true);
            CellStyle totalDecimalStyle = createNumberStyle(workbook, "#,##0.00", true);
            CellStyle totalTexteStyle = createTotalTextStyle(workbook);

            for (Map.Entry<String, List<SectionExcel>> feuille : feuilles.entrySet()) {
                SXSSFSheet sheet = workbook.createSheet(sanitizeSheetName(feuille.getKey()));
                sheet.trackAllColumnsForAutoSizing();
                int largeur = feuille.getValue().stream()
                        .mapToInt(section -> section.entetes().length)
                        .max()
                        .orElse(1);
                int rowNum = 0;

                Cell titleCell = sheet.createRow(rowNum++).createCell(0);
                titleCell.setCellValue(title);
                titleCell.setCellStyle(titleStyle);
                Cell dateCell = sheet.createRow(rowNum++).createCell(0);
                dateCell.setCellValue("Généré le: " + LocalDateTime.now().format(DATETIME_FORMATTER));
                dateCell.setCellStyle(dateStyle);
                if (largeur > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, largeur - 1));
                    sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, largeur - 1));
                }

                for (SectionExcel section : feuille.getValue()) {
                    rowNum++;
                    Cell sectionCell = sheet.createRow(rowNum++).createCell(0);
                    sectionCell.setCellValue(section.titre());
                    sectionCell.setCellStyle(sectionStyle);
                    Row headerRow = sheet.createRow(rowNum++);
                    for (int i = 0; i < section.entetes().length; i++) {
                        Cell cell = headerRow.createCell(i);
                        cell.setCellValue(section.entetes()[i]);
                        cell.setCellStyle(headerStyle);
                    }
                    for (Object[] ligne : section.lignes()) {
                        writeValues(sheet.createRow(rowNum++), ligne, entierStyle, decimalStyle, null);
                    }
                    if (section.total() != null) {
                        writeValues(
                                sheet.createRow(rowNum++),
                                section.total(),
                                totalEntierStyle,
                                totalDecimalStyle,
                                totalTexteStyle);
                    }
                }
                for (int i = 0; i < largeur; i++) {
                    sheet.autoSizeColumn(i);
                }
            }
            workbook.write(out);
            workbook.dispose();
            return out.toByteArray();
        }
    }

    /** Un nombre entier ou décimal à son format ; le reste en texte, avec le style donné s'il y en a un. */
    private void writeValues(
            Row row, Object[] valeurs, CellStyle entierStyle, CellStyle decimalStyle, CellStyle texteStyle) {
        for (int i = 0; i < valeurs.length; i++) {
            Object valeur = valeurs[i];
            if (valeur == null) {
                continue;
            }
            Cell cell = row.createCell(i);
            if (valeur instanceof Number nombre) {
                cell.setCellValue(nombre.doubleValue());
                boolean entier = valeur instanceof Integer || valeur instanceof Long || valeur instanceof Short;
                cell.setCellStyle(entier ? entierStyle : decimalStyle);
            } else {
                cell.setCellValue(valeur.toString());
                if (texteStyle != null) {
                    cell.setCellStyle(texteStyle);
                }
            }
        }
    }

    // Styles

    private CellStyle createSectionStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createNumberStyle(SXSSFWorkbook workbook, String format, boolean gras) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat(format));
        if (gras) {
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
        }
        return style;
    }

    private CellStyle createTotalTextStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createTitleStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    // Utilitaires

    private String sanitizeSheetName(String name) {
        // Les noms de feuilles Excel ne peuvent pas contenir : \ / ? * [ ]
        return name.replaceAll("[:\\\\/?*\\[\\]]", "_").substring(0, Math.min(name.length(), 31));
    }

    /**
     * Formate un nombre avec 2 décimales
     */
    public String formatNumber(Number value) {
        if (value == null) return "0.00";
        return String.format("%.2f", value.doubleValue());
    }

    /**
     * Formate une date
     */
    public String formatDate(LocalDateTime date) {
        if (date == null) return "";
        return date.format(DATE_FORMATTER);
    }
}
