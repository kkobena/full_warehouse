package com.kobe.warehouse.service.report.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.service.dto.report.DailyCADTO;
import com.kobe.warehouse.service.dto.report.TopProductDTO;
import com.kobe.warehouse.service.report.DashboardCAService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Deux rapports du tableau de bord s'exportent chacun sous deux formes, Excel et CSV. Les quatre
 * méthodes écrivent leur mappage de colonnes <b>à la main, deux fois</b> — une fois en cellules POI,
 * une fois en tableau de chaînes.
 *
 * <p>C'est là que se loge le défaut propre à cette famille : rien n'oblige les deux mappages à
 * rester d'accord. Une colonne ajoutée d'un côté et pas de l'autre, et le CSV du même écran décale
 * toutes ses valeurs d'un rang — sans erreur, sans avertissement, et personne ne compare jamais les
 * deux fichiers. Ces tests les comparent.
 */
@DisplayName("DashboardCAPExcelCsvExportService — exports du tableau de bord")
class DashboardCAPExcelCsvExportServiceTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 1, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 3, 31);

    // Les doublures sont créées à l'initialisation du champ, et non dans un @BeforeEach : le délai
    // de quinze secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito, qui
    // restait ensuite inutilisable pour toute la classe.
    private final DashboardCAService dashboardCAService = mock(DashboardCAService.class);

    private final DashboardCAPExcelCsvExportService service = new DashboardCAPExcelCsvExportService(
        new ReportExcelExportService(),
        new CsvExportService(),
        dashboardCAService
    );

    // Les quatre fichiers sont produits une seule fois : le jeu d'essai est fixe, et
    // l'auto-dimensionnement des colonnes de POI charge les métriques de police à chaque classeur —
    // les réexporter à chaque test multipliait la durée de la classe par quatre.
    private static byte[] journalierExcel;
    private static byte[] journalierCsv;
    private static byte[] palmaresExcel;
    private static byte[] palmaresCsv;

    @BeforeEach
    void setUp() throws Exception {
        when(dashboardCAService.getDailySummary(any(), any())).thenReturn(List.of(journee()));
        when(dashboardCAService.getTopProducts(any(), any(), any())).thenReturn(List.of(produit()));

        if (journalierExcel == null) {
            journalierExcel = service.exportDailySummaryToExcel(DEBUT, FIN);
            journalierCsv = service.exportDailySummaryToCsv(DEBUT, FIN);
            palmaresExcel = service.exportTopProductsToExcel(DEBUT, FIN);
            palmaresCsv = service.exportTopProductsToCsv(DEBUT, FIN);
        }
    }

    // ===== chiffre d'affaires journalier =====

    @Nested
    @DisplayName("Chiffre d'affaires journalier")
    class ChiffreDAffairesJournalier {

        @Test
        @DisplayName("Excel et CSV portent les mêmes colonnes, dans le même ordre")
        void memesColonnes() throws Exception {
            assertThat(entetesDuClasseur(journalierExcel))
                .isEqualTo(entetesDuCsv(journalierCsv));
        }

        /**
         * Le vrai piège : deux en-têtes — « Nb Avoirs » et « CA Avoirs » — n'avaient aucune valeur
         * correspondante, le DTO ne portant pas d'avoirs. Treize colonnes annoncées pour onze
         * écrites décalaient tout le tableau, et le montant lu sous « CA Total » était en fait le CA
         * net.
         */
        @Test
        @DisplayName("il y a autant de valeurs que d'en-têtes annoncés")
        void memeNombreDeValeurs() throws Exception {
            List<String> entetes = entetesDuClasseur(journalierExcel);

            assertThat(premiereDonneeDuCsv(journalierCsv)).hasSize(entetes.size());
        }

        @Test
        @DisplayName("chaque valeur se lit sous l'en-tête qui la désigne")
        void valeursSousLeBonEntete() throws Exception {
            List<String> entetes = entetesDuCsv(journalierCsv);
            List<String> valeurs = premiereDonneeDuCsv(journalierCsv);

            assertThat(valeurs.get(entetes.indexOf("CA Total"))).isEqualTo("1000000");
            assertThat(valeurs.get(entetes.indexOf("CA Net"))).isEqualTo("950000");
            assertThat(valeurs.get(entetes.indexOf("Marge Brute"))).isEqualTo("400000");
            assertThat(valeurs.get(entetes.indexOf("Crédit"))).isEqualTo("100000");
        }

        /**
         * Le nom de feuille Excel est tronqué à trente-et-un caractères ; le titre du CSV, lui, est
         * complet. C'est donc sur ce dernier qu'on lit la période en entier.
         */
        @Test
        @DisplayName("le titre porte la période analysée")
        void titreAvecPeriode() throws Exception {
            assertThat(premiereLigneDuCsv(journalierCsv))
                .isEqualTo("Chiffre d'Affaires Journalier 01-01-2026 au 31-03-2026");
        }

        @Test
        @DisplayName("le CSV porte la marque UTF-8 attendue par Excel")
        void marqueUtf8() throws Exception {
            assertThat(journalierCsv).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        }
    }

    // ===== palmarès des produits =====

    @Nested
    @DisplayName("Palmarès des produits")
    class PalmaresDesProduits {

        @Test
        @DisplayName("Excel et CSV portent les mêmes colonnes, dans le même ordre")
        void memesColonnes() throws Exception {
            assertThat(entetesDuClasseur(palmaresExcel))
                .isEqualTo(entetesDuCsv(palmaresCsv));
        }

        @Test
        @DisplayName("Excel et CSV portent le même nombre de valeurs par ligne")
        void memeNombreDeValeurs() throws Exception {
            int colonnes = entetesDuClasseur(palmaresExcel).size();

            assertThat(premiereDonneeDuCsv(palmaresCsv)).hasSize(colonnes);
        }

        /**
         * Les deux formes composent leur titre séparément, et le CSV oubliait l'espace avant la
         * période : « Top Produits par CA01-01-2026 ». Le défaut était invisible tant que le titre
         * n'apparaissait pas dans le fichier CSV.
         */
        @Test
        @DisplayName("Excel et CSV portent le même titre")
        void memeTitre() throws Exception {
            String titreExcel = nomDeLaFeuille(palmaresExcel);
            String titreCsv = premiereLigneDuCsv(palmaresCsv);

            assertThat(titreCsv).isEqualTo("Top Produits par CA 01-01-2026 au 31-03-2026");
            // Le nom de feuille Excel est tronqué à 31 caractères : il en est donc un préfixe.
            assertThat(titreCsv).startsWith(titreExcel);
        }

        @Test
        @DisplayName("le palmarès demande les cinquante premiers produits")
        void cinquantePremiers() throws Exception {
            service.exportTopProductsToCsv(DEBUT, FIN);

            org.mockito.Mockito.verify(dashboardCAService).getTopProducts(DEBUT, FIN, 50);
        }
    }

    // ===== lecture des fichiers produits =====

    private static List<String> entetesDuClasseur(byte[] classeur) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(classeur))) {
            Sheet feuille = workbook.getSheetAt(0);
            return java.util.stream.StreamSupport.stream(feuille.getRow(3).spliterator(), false)
                .map(cellule -> cellule.getStringCellValue())
                .toList();
        }
    }

    private static String nomDeLaFeuille(byte[] classeur) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(classeur))) {
            return workbook.getSheetAt(0).getSheetName();
        }
    }

    private static List<String> lignesDuCsv(byte[] csv) {
        // La marque UTF-8 précède le contenu : on la retire avant de lire.
        String contenu = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
        return contenu.lines().filter(l -> !l.isBlank()).toList();
    }

    private static String premiereLigneDuCsv(byte[] csv) {
        return lignesDuCsv(csv).getFirst();
    }

    /** Le titre et la date précèdent les en-têtes : ceux-ci sont donc la troisième ligne. */
    private static List<String> entetesDuCsv(byte[] csv) {
        return List.of(lignesDuCsv(csv).get(2).split(";"));
    }

    private static List<String> premiereDonneeDuCsv(byte[] csv) {
        return List.of(lignesDuCsv(csv).get(3).split(";", -1));
    }

    // ===== fabriques =====

    private static DailyCADTO journee() {
        return new DailyCADTO(
            LocalDate.of(2026, 1, 15), 42, 1_000_000L, 950_000L, BigDecimal.valueOf(23809), 600_000L, 400_000L,
            BigDecimal.valueOf(40), 38, 900_000L, 100_000L
        );
    }

    private static TopProductDTO produit() {
        return new TopProductDTO(LocalDate.of(2026, 1, 1), 1, "DOLIPRANE", "CIP1", 12L, 340, 3_400_000, BigDecimal.valueOf(10000));
    }
}
