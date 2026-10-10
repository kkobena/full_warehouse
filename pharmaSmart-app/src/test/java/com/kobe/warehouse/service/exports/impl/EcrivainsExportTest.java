package com.kobe.warehouse.service.exports.impl;

import static com.kobe.warehouse.domain.enumeration.TypeColonneExport.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.enumeration.ExportDonnees.ColonneExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.service.exports.EcrivainExport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Exports — écriture CSV et XLSX")
class EcrivainsExportTest {

    private static final List<ColonneExport> COLONNES = List.of(
        new ColonneExport("Date", DATE),
        new ColonneExport("Produit", TEXTE),
        new ColonneExport("Quantité", ENTIER),
        new ColonneExport("Prix", DECIMAL),
        new ColonneExport("Créée le", DATE_HEURE)
    );
    private static final Object[] LIGNE = { LocalDate.of(2026, 10, 9), "Doliprane \"1000\"; boîte", 3, 1250.5, LocalDateTime.of(2026, 10, 9, 14, 5) };

    @Test
    @DisplayName("CSV : BOM, « ; », guillemets doublés, virgule décimale, dates à la française, vide pour null")
    void csv() throws Exception {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try (EcrivainExport ecrivain = new FabriqueEcrivainsExportImpl().ouvrir(FormatExport.CSV, COLONNES, sortie)) {
            ecrivain.ecrire(LIGNE);
            ecrivain.ecrire(new Object[] { null, "Sans date", null, null, null });
        }
        String contenu = sortie.toString(StandardCharsets.UTF_8);

        assertThat(contenu).startsWith("\uFEFF\"Date\";\"Produit\";\"Quantité\";\"Prix\";\"Créée le\"\r\n");
        assertThat(contenu).contains("09/10/2026;\"Doliprane \"\"1000\"\"; boîte\";3;1250,5;09/10/2026 14:05\r\n");
        assertThat(contenu).endsWith(";\"Sans date\";;;\r\n");
    }

    @Test
    @DisplayName("XLSX : en-tête, nombres et dates en vraies cellules")
    void xlsx() throws Exception {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try (EcrivainExport ecrivain = new FabriqueEcrivainsExportImpl().ouvrir(FormatExport.XLSX, COLONNES, sortie)) {
            ecrivain.ecrire(LIGNE);
        }

        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(sortie.toByteArray()))) {
            Sheet feuille = classeur.getSheetAt(0);
            assertThat(feuille.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Produit");
            assertThat(feuille.getRow(1).getCell(0).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 10, 9));
            assertThat(feuille.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(3);
            assertThat(feuille.getRow(1).getCell(3).getNumericCellValue()).isEqualTo(1250.5);
        }
    }
}
