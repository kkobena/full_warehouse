package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.ExportDonnees.ColonneExport;
import com.kobe.warehouse.domain.enumeration.TypeColonneExport;
import com.kobe.warehouse.service.exports.EcrivainExport;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * XLSX en flux (POI SXSSF, comme l'export comptable) : seules quelques centaines de lignes restent en mémoire ; nombres et dates
 * en vraies cellules numériques. Au-delà de la limite d'une feuille, la suite part sur une nouvelle feuille.
 */
class EcrivainXlsx implements EcrivainExport {

    private static final int LIGNES_EN_MEMOIRE = 200;
    private static final int LIGNES_PAR_FEUILLE = 1_000_000;

    private final List<ColonneExport> colonnes;
    private final OutputStream sortie;
    private final SXSSFWorkbook classeur = new SXSSFWorkbook(LIGNES_EN_MEMOIRE);
    private final Map<TypeColonneExport, CellStyle> styles = new EnumMap<>(TypeColonneExport.class);
    private final CellStyle styleEntete;
    private Sheet feuille;
    private int rangLigne;
    private int numeroFeuille;

    EcrivainXlsx(List<ColonneExport> colonnes, OutputStream sortie) {
        this.colonnes = colonnes;
        this.sortie = sortie;
        classeur.setCompressTempFiles(true);
        var format = classeur.createDataFormat();
        styles.put(TypeColonneExport.ENTIER, style(format.getFormat("#,##0")));
        styles.put(TypeColonneExport.MONTANT, style(format.getFormat("#,##0")));
        styles.put(TypeColonneExport.DECIMAL, style(format.getFormat("#,##0.00")));
        styles.put(TypeColonneExport.DATE, style(format.getFormat("dd/mm/yyyy")));
        styles.put(TypeColonneExport.DATE_HEURE, style(format.getFormat("dd/mm/yyyy hh:mm")));
        styleEntete = classeur.createCellStyle();
        Font gras = classeur.createFont();
        gras.setBold(true);
        styleEntete.setFont(gras);
        ouvrirFeuille();
    }

    @Override
    public void ecrire(Object[] ligne) {
        if (rangLigne > LIGNES_PAR_FEUILLE) {
            ouvrirFeuille();
        }
        Row row = feuille.createRow(rangLigne++);
        for (int rang = 0; rang < colonnes.size() && rang < ligne.length; rang++) {
            remplir(row.createCell(rang), ligne[rang], colonnes.get(rang).type());
        }
    }

    private void remplir(Cell cellule, Object valeur, TypeColonneExport type) {
        switch (valeur) {
            case null -> cellule.setBlank();
            case LocalDateTime horodatage -> cellule.setCellValue(horodatage);
            case LocalDate date -> cellule.setCellValue(date);
            case Number nombre -> cellule.setCellValue(nombre.doubleValue());
            case Boolean booleen -> cellule.setCellValue(booleen ? "Oui" : "Non");
            default -> cellule.setCellValue(valeur.toString());
        }
        CellStyle style = styles.get(type);
        if (style != null && valeur != null) {
            cellule.setCellStyle(style);
        }
    }

    private void ouvrirFeuille() {
        feuille = classeur.createSheet(numeroFeuille == 0 ? "Export" : "Export (suite " + numeroFeuille + ")");
        numeroFeuille++;
        feuille.createFreezePane(0, 1);
        Row entete = feuille.createRow(0);
        for (int rang = 0; rang < colonnes.size(); rang++) {
            Cell cellule = entete.createCell(rang);
            cellule.setCellValue(colonnes.get(rang).libelle());
            cellule.setCellStyle(styleEntete);
        }
        rangLigne = 1;
    }

    private CellStyle style(short format) {
        CellStyle style = classeur.createCellStyle();
        style.setDataFormat(format);
        return style;
    }

    @Override
    public void close() throws IOException {
        try (classeur) {
            classeur.write(sortie);
        } finally {
            classeur.dispose();
        }
    }
}
