package com.kobe.warehouse.service.report.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tous les exports Excel du logiciel passent par ce service. Le classeur produit suit une
 * disposition fixe — titre, date de génération, ligne vide, en-têtes, puis les données — que le
 * lecteur retrouve d'un rapport à l'autre.
 *
 * <p>Ce qui s'y vérifie n'est pas la mise en forme mais le <b>placement</b> : une ligne de décalage
 * et les en-têtes se retrouvent au milieu des données. Et le <b>nom de la feuille</b>, qu'Excel
 * refuse au-delà de trente-et-un caractères ou s'il contient l'un des six caractères réservés —
 * refus qui se traduit par une exception au moment de l'écriture, donc par un export qui échoue
 * sans que l'utilisateur comprenne pourquoi.
 */
@DisplayName("ReportExcelExportService — socle des exports Excel")
class ReportExcelExportServiceTest {

    private ReportExcelExportService service;

    @BeforeEach
    void setUp() {
        service = new ReportExcelExportService();
    }

    // ===== disposition du classeur =====

    @Nested
    @DisplayName("Disposition du classeur")
    class DispositionDuClasseur {

        @Test
        @DisplayName("le titre ouvre la feuille et la date le suit")
        void titrePuisDate() throws IOException {
            Sheet feuille = feuille(classeur("Alertes Stock", entetes("Code", "Libellé"), lignes()));

            assertThat(valeur(feuille, 0, 0)).isEqualTo("Alertes Stock");
            assertThat(valeur(feuille, 1, 0)).startsWith("Généré le:");
        }

        /** Une ligne vide sépare l'en-tête du document du tableau : les en-têtes sont donc en ligne 4. */
        @Test
        @DisplayName("les en-têtes viennent après une ligne vide")
        void entetesApresUneLigneVide() throws IOException {
            Sheet feuille = feuille(classeur("Alertes", entetes("Code", "Libellé", "Stock"), lignes()));

            assertThat(feuille.getRow(2)).isNull();
            assertThat(valeur(feuille, 3, 0)).isEqualTo("Code");
            assertThat(valeur(feuille, 3, 1)).isEqualTo("Libellé");
            assertThat(valeur(feuille, 3, 2)).isEqualTo("Stock");
        }

        @Test
        @DisplayName("les données suivent immédiatement les en-têtes")
        void donneesApresLesEntetes() throws IOException {
            Sheet feuille = feuille(
                classeur("Alertes", entetes("Code", "Libellé"), lignes(ligne("CIP1", "DOLIPRANE"), ligne("CIP2", "EFFERALGAN")))
            );

            assertThat(valeur(feuille, 4, 0)).isEqualTo("CIP1");
            assertThat(valeur(feuille, 4, 1)).isEqualTo("DOLIPRANE");
            assertThat(valeur(feuille, 5, 0)).isEqualTo("CIP2");
            assertThat(feuille.getLastRowNum()).isEqualTo(5);
        }

        @Test
        @DisplayName("un export sans donnée conserve son titre et ses en-têtes")
        void exportSansDonnee() throws IOException {
            Sheet feuille = feuille(classeur("Alertes", entetes("Code", "Libellé"), lignes()));

            assertThat(valeur(feuille, 0, 0)).isEqualTo("Alertes");
            assertThat(valeur(feuille, 3, 0)).isEqualTo("Code");
            assertThat(feuille.getLastRowNum()).isEqualTo(3);
        }

        /** Le titre et la date sont fusionnés sur toute la largeur du tableau. */
        @Test
        @DisplayName("le titre et la date couvrent toute la largeur du tableau")
        void titreEtDateFusionnes() throws IOException {
            Sheet feuille = feuille(classeur("Alertes", entetes("A", "B", "C", "D"), lignes()));

            assertThat(feuille.getMergedRegions()).hasSize(2);
            assertThat(feuille.getMergedRegion(0).getLastColumn()).isEqualTo(3);
        }

        @Test
        @DisplayName("les accents survivent au classeur")
        void accents() throws IOException {
            Sheet feuille = feuille(classeur("Produits périmés", entetes("Libellé", "Péremption"), lignes(ligne("DOLIPRANE", "31/03/2026"))));

            assertThat(valeur(feuille, 0, 0)).isEqualTo("Produits périmés");
            assertThat(valeur(feuille, 3, 0)).isEqualTo("Libellé");
        }

        /**
         * Un rapport à colonne unique n'a rien à fusionner, et POI refuse une région fusionnée d'une
         * seule cellule : l'export échouait à l'écriture, sans que l'utilisateur récupère de fichier.
         */
        @Test
        @DisplayName("un rapport à colonne unique s'exporte sans fusion")
        void colonneUnique() throws IOException {
            Sheet feuille = feuille(classeur("Codes CIP", entetes("Code"), lignes(ligne("CIP1"), ligne("CIP2"))));

            assertThat(feuille.getMergedRegions()).isEmpty();
            assertThat(valeur(feuille, 0, 0)).isEqualTo("Codes CIP");
            assertThat(valeur(feuille, 3, 0)).isEqualTo("Code");
            assertThat(valeur(feuille, 4, 0)).isEqualTo("CIP1");
        }
    }

    // ===== nom de la feuille =====

    @Nested
    @DisplayName("Nom de la feuille")
    class NomDeLaFeuille {

        @Test
        @DisplayName("le titre nomme la feuille")
        void titreNommeLaFeuille() throws IOException {
            assertThat(feuille(classeur("Alertes Stock", entetes("Code", "Libellé"), lignes())).getSheetName()).isEqualTo("Alertes Stock");
        }

        /**
         * Excel refuse les six caractères réservés dans un nom de feuille, et un titre de rapport
         * porte souvent une période — « du 01/01/2026 au 31/03/2026 ». Sans remplacement, l'export
         * échoue à l'écriture et l'utilisateur ne récupère aucun fichier.
         */
        @Test
        @DisplayName("les caractères qu'Excel refuse sont remplacés")
        void caracteresInterdits() throws IOException {
            String nom = feuille(classeur("Ventes 01/01 [A]*?", entetes("Code", "Libellé"), lignes())).getSheetName();

            assertThat(nom).doesNotContain("/").doesNotContain("[").doesNotContain("]").doesNotContain("*").doesNotContain("?");
            assertThat(nom).isEqualTo("Ventes 01_01 _A___");
        }

        /** Excel plafonne à trente-et-un caractères ; au-delà, l'écriture échoue. */
        @Test
        @DisplayName("un titre trop long est tronqué à la limite d'Excel")
        void titreTropLong() throws IOException {
            String titreLong = "Récapitulatif des produits vendus du 01/01/2026 au 31/03/2026";

            String nom = feuille(classeur(titreLong, entetes("Code", "Libellé"), lignes())).getSheetName();

            assertThat(nom).hasSize(31);
        }
    }

    // ===== formatage =====

    @Nested
    @DisplayName("Formatage")
    class Formatage {

        /**
         * Le séparateur décimal suit la locale de la machine : « 1234,50 » sur un poste français,
         * « 1234.50 » sur un serveur anglophone. Le test l'admet plutôt que de figer l'un des deux —
         * il tournerait sinon au vert ici et au rouge sur une intégration continue anglophone.
         */
        @Test
        @DisplayName("un nombre s'écrit avec deux décimales")
        void deuxDecimales() {
            assertThat(service.formatNumber(1234.5)).matches("1234[.,]50");
            assertThat(service.formatNumber(0)).matches("0[.,]00");
            assertThat(service.formatNumber(1234.567)).matches("1234[.,]57");
        }

        /** Une valeur absente s'écrit zéro plutôt que de laisser un trou au milieu d'une colonne de montants. */
        @Test
        @DisplayName("un nombre absent s'écrit zéro")
        void nombreAbsent() {
            assertThat(service.formatNumber(null)).isEqualTo("0.00");
        }

        @Test
        @DisplayName("une date s'écrit au format du jour")
        void formatDeLaDate() {
            assertThat(service.formatDate(LocalDateTime.of(2026, 3, 31, 14, 30))).isEqualTo("31/03/2026");
        }

        @Test
        @DisplayName("une date absente laisse la cellule vide")
        void dateAbsente() {
            assertThat(service.formatDate(null)).isEmpty();
        }
    }

    // ===== fabriques =====

    private byte[] classeur(String titre, String[] entetes, List<String[]> donnees) throws IOException {
        return service.createSimpleExcelReport(titre, entetes, donnees);
    }

    private static Sheet feuille(byte[] classeur) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(classeur))) {
            return workbook.getSheetAt(0);
        }
    }

    private static String valeur(Sheet feuille, int ligne, int colonne) {
        Row row = feuille.getRow(ligne);
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(colonne);
        return cell == null ? null : cell.getStringCellValue();
    }

    private static String[] entetes(String... entetes) {
        return entetes;
    }

    private static String[] ligne(String... valeurs) {
        return valeurs;
    }

    private static List<String[]> lignes(String[]... lignes) {
        return List.of(lignes);
    }
}
