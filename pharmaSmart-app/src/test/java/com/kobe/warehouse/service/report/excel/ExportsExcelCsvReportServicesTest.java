package com.kobe.warehouse.service.report.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.domain.enumeration.StockAlertType;
import com.kobe.warehouse.service.dto.RetourBonDTO;
import com.kobe.warehouse.service.dto.RetourBonItemDTO;
import com.kobe.warehouse.service.dto.report.StockAlertDTO;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyDTO;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyFilter;
import com.kobe.warehouse.service.product_to_destroy.service.ProductsToDestroyService;
import com.kobe.warehouse.service.report.StockAlertReportService;
import com.kobe.warehouse.service.sale.SaleDataService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.RetourBonService;
import com.kobe.warehouse.service.stock.dto.LotFilterParam;
import com.kobe.warehouse.service.stock.dto.LotPerimeDTO;
import com.kobe.warehouse.service.stock.dto.StockDepotExportDTO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Cinq rapports s'exportent sous deux formes, et chacun écrit son mappage de colonnes <b>deux fois</b>
 * — une fois en cellules POI pour Excel, une fois en tableau de chaînes pour le CSV. Les deux
 * mappages doivent rester d'accord entre eux, et d'accord avec le tableau d'en-têtes qu'ils
 * partagent.
 *
 * <p>Rien ne les y oblige, et un désaccord ne se signale d'aucune façon : le fichier s'écrit, se
 * télécharge et s'ouvre. C'est ainsi que l'export du chiffre d'affaires journalier a pu annoncer
 * treize colonnes pour onze valeurs, et décaler tout son tableau. Ces tests appliquent aux cinq
 * autres la vérification qui l'a mis au jour : <b>autant de valeurs que d'en-têtes, les mêmes des
 * deux côtés</b>.
 *
 * <p>Les titres sont éprouvés sur leurs quatre formes — période bornée, ouverte d'un côté ou de
 * l'autre, absente : c'est ce qui distingue deux exports du même rapport posés côte à côte.
 */
@DisplayName("Exports Excel et CSV — parité des colonnes")
class ExportsExcelCsvReportServicesTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 1, 5);
    private static final LocalDate FIN = LocalDate.of(2026, 3, 31);

    private final ReportExcelExportService excel = new ReportExcelExportService();
    private final CsvExportService csv = new CsvExportService();

    // ===== alertes de stock =====

    @Nested
    @DisplayName("Alertes de stock")
    class AlertesDeStock {

        private final StockAlertReportService reportService = mock(StockAlertReportService.class);
        private final StockAlertExcelCsvReportService service = new StockAlertExcelCsvReportService(excel, csv, reportService);

        @Test
        @DisplayName("Excel et CSV portent les mêmes colonnes, toutes renseignées")
        void parite() throws Exception {
            when(reportService.getStockAlerts(any(), any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(new StockAlertDTO(1, "DOLIPRANE", "CIP1", 3, 10, LocalDate.of(2026, 6, 30), StockAlertType.ALERTE)))
            );
            when(reportService.getStockAlertsCount()).thenReturn(java.util.Map.of());

            verifieLaParite(service.exportToExcel(null), service.exportToCsv(null));
        }

        /** Les types demandés sont rappelés dans le titre : deux exports filtrés se distinguent ainsi. */
        @Test
        @DisplayName("les types filtrés sont rappelés dans le titre")
        void titreAvecFiltre() throws Exception {
            when(reportService.getStockAlerts(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
            when(reportService.getStockAlertsCount()).thenReturn(java.util.Map.of());

            assertThat(titreDuCsv(service.exportToCsv(List.of(StockAlertType.RUPTURE, StockAlertType.ALERTE))))
                .isEqualTo("Alertes Stock - RUPTURE, ALERTE");
            assertThat(titreDuCsv(service.exportToCsv(null))).isEqualTo("Alertes Stock");
        }
    }

    // ===== vente dépôt =====

    @Nested
    @DisplayName("Vente dépôt")
    class VenteDepot {

        private final SaleDataService saleDataService = mock(SaleDataService.class);
        private final StockDepotExcelCsvReportService service = new StockDepotExcelCsvReportService(excel, csv, saleDataService);

        @Test
        @DisplayName("Excel et CSV portent les mêmes colonnes, toutes renseignées")
        void parite() throws Exception {
            when(saleDataService.exportVenteDepotStock(any())).thenReturn(List.of(ligneDepot()));

            verifieLaParite(service.exportToExcel(venteId()), service.exportToCsv(venteId()));
        }

        @Test
        @DisplayName("le titre nomme la vente et sa date")
        void titre() throws Exception {
            when(saleDataService.exportVenteDepotStock(any())).thenReturn(List.of());

            assertThat(titreDuCsv(service.exportToCsv(venteId()))).isEqualTo("Vente Dépôt Stock - 42 du 05-01-2026");
        }

        private com.kobe.warehouse.domain.SaleId venteId() {
            return new com.kobe.warehouse.domain.SaleId(42L, DEBUT);
        }

        private StockDepotExportDTO ligneDepot() {
            StockDepotExportDTO dto = new StockDepotExportDTO();
            dto.setProduitId(1);
            dto.setCode("CIP1");
            dto.setProduitLibelle("DOLIPRANE");
            dto.setCodeEan("EAN1");
            dto.setQuantitySold(10);
            dto.setQuantityRequested(12);
            dto.setRegularUnitPrice(10_000);
            dto.setTaxValue(0);
            dto.setCostAmount(6_000);
            return dto;
        }
    }

    // ===== lots périmés =====

    @Nested
    @DisplayName("Lots périmés")
    class LotsPerimes {

        private final LotService lotService = mock(LotService.class);
        private final LotPerimeExcelCsvReportService service = new LotPerimeExcelCsvReportService(excel, csv, lotService);

        @Test
        @DisplayName("Excel et CSV portent les mêmes colonnes, toutes renseignées")
        void parite() throws Exception {
            when(lotService.findLotsPerimes(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(lotPerime())));

            verifieLaParite(service.exportToExcel(filtre(DEBUT, FIN)), service.exportToCsv(filtre(DEBUT, FIN)));
        }

        @Test
        @DisplayName("le titre porte la période, sous ses quatre formes")
        void titreSelonLaPeriode() throws Exception {
            when(lotService.findLotsPerimes(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

            assertThat(titreDuCsv(service.exportToCsv(filtre(DEBUT, FIN))))
                .isEqualTo("Liste des produits périmés du 05-01-2026 au 31-03-2026");
            assertThat(titreDuCsv(service.exportToCsv(filtre(DEBUT, null))))
                .isEqualTo("Liste des produits périmés à partir du 05-01-2026");
            assertThat(titreDuCsv(service.exportToCsv(filtre(null, FIN))))
                .isEqualTo("Liste des produits périmés jusqu'au 31-03-2026");
            assertThat(titreDuCsv(service.exportToCsv(filtre(null, null)))).isEqualTo("Liste des produits périmés");
        }

        private LotFilterParam filtre(LocalDate debut, LocalDate fin) {
            LotFilterParam filtre = new LotFilterParam();
            filtre.setFromDate(debut);
            filtre.setToDate(fin);
            return filtre;
        }

        private LotPerimeDTO lotPerime() {
            LotPerimeDTO dto = new LotPerimeDTO();
            dto.setNumLot("LOT-1");
            dto.setFournisseur("LABOREX");
            dto.setProduitName("DOLIPRANE");
            dto.setProduitCode("CIP1");
            dto.setDatePeremption("30/06/2026");
            dto.setQuantity(25);
            dto.setPrixAchat(6_000);
            dto.setPrixVente(10_000);
            dto.setPrixTotalVente(250_000);
            dto.setPrixTotaAchat(150_000);
            dto.setStatutPerime("PERIME");
            dto.setRayonName("COMPTOIR");
            dto.setFamilleProduitName("MEDICAMENT");
            return dto;
        }
    }

    // ===== produits à détruire =====

    @Nested
    @DisplayName("Produits à détruire")
    class ProduitsADetruire {

        private final ProductsToDestroyService productsToDestroyService = mock(ProductsToDestroyService.class);
        private final ProductToDestroyExcelCsvReportService service = new ProductToDestroyExcelCsvReportService(
            excel,
            csv,
            productsToDestroyService
        );

        @Test
        @DisplayName("Excel et CSV portent les mêmes colonnes, toutes renseignées")
        void parite() throws Exception {
            when(productsToDestroyService.findAll(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(produitADetruire())));

            verifieLaParite(service.exportToExcel(filtre(DEBUT, FIN)), service.exportToCsv(filtre(DEBUT, FIN)));
        }

        /** « Stock déjà détruit » se lit en toutes lettres : un booléen brut ne dirait rien au lecteur. */
        @Test
        @DisplayName("l'état de destruction se lit en toutes lettres")
        void etatDeDestruction() throws Exception {
            when(productsToDestroyService.findAll(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(produitADetruire())));

            List<String> entetes = entetesDuCsv(service.exportToCsv(filtre(DEBUT, FIN)));
            List<String> valeurs = premiereDonneeDuCsv(service.exportToCsv(filtre(DEBUT, FIN)));

            assertThat(valeurs.get(entetes.indexOf("Stock déjà détruit"))).isEqualTo("Oui");
        }

        private ProductToDestroyFilter filtre(LocalDate debut, LocalDate fin) {
            return new ProductToDestroyFilter(debut, fin, null, null, null, null, null, null, null);
        }

        private ProductToDestroyDTO produitADetruire() {
            ProductToDestroyDTO dto = new ProductToDestroyDTO();
            dto.setProduitName("DOLIPRANE");
            dto.setProduitCodeCip("CIP1");
            dto.setNumLot("LOT-1");
            dto.setQuantity(25);
            dto.setDatePeremption("30/06/2026");
            dto.setDateDestruction("15/07/2026");
            dto.setUser("System");
            dto.setCreatedDate("01/01/2026");
            dto.setUpdatedDate("02/01/2026");
            dto.setFournisseur("LABOREX");
            dto.setPrixAchat(6_000);
            dto.setPrixUni(10_000);
            dto.setDestroyed(true);
            return dto;
        }
    }

    // ===== retours fournisseur =====

    @Nested
    @DisplayName("Retours fournisseur")
    class RetoursFournisseur {

        private final RetourBonService retourBonService = mock(RetourBonService.class);
        private final RetourBonExcelCsvReportService service = new RetourBonExcelCsvReportService(excel, csv, retourBonService);

        @Test
        @DisplayName("Excel et CSV portent les mêmes colonnes, toutes renseignées")
        void parite() throws Exception {
            bonsRetournes(bon("BR-1", ligneRetour("DOLIPRANE", 10, 6_000)));

            verifieLaParite(
                service.exportToExcel(null, DEBUT, FIN, null),
                service.exportToCsv(null, DEBUT, FIN, null)
            );
        }

        /**
         * Un bon porte plusieurs lignes ; le rapport les aplatit, une ligne de produit par ligne de
         * tableau. Deux bons de deux lignes font donc quatre lignes, et non deux.
         */
        @Test
        @DisplayName("chaque ligne de produit devient une ligne du tableau")
        void aplatissementDesLignes() throws Exception {
            bonsRetournes(
                bon("BR-1", ligneRetour("DOLIPRANE", 10, 6_000), ligneRetour("EFFERALGAN", 5, 4_000)),
                bon("BR-2", ligneRetour("ZOVIRAX", 2, 12_000), ligneRetour("MOTILIUM", 3, 8_000))
            );

            assertThat(donneesDuCsv(service.exportToCsv(null, DEBUT, FIN, null))).hasSize(4);
        }

        /** Le montant du retour est le prix d'achat multiplié par la quantité renvoyée. */
        @Test
        @DisplayName("le montant du retour valorise la quantité au prix d'achat")
        void montantDuRetour() throws Exception {
            bonsRetournes(bon("BR-1", ligneRetour("DOLIPRANE", 10, 6_000)));

            List<String> entetes = entetesDuCsv(service.exportToCsv(null, DEBUT, FIN, null));
            List<String> valeurs = premiereDonneeDuCsv(service.exportToCsv(null, DEBUT, FIN, null));

            assertThat(valeurs.get(entetes.indexOf("Montant retour"))).isEqualTo("60000");
        }

        @Test
        @DisplayName("le titre porte la période, sous ses quatre formes")
        void titreSelonLaPeriode() throws Exception {
            bonsRetournes();

            assertThat(titreDuCsv(service.exportToCsv(null, DEBUT, FIN, null)))
                .isEqualTo("Liste des retours fournisseur du 05-01-2026 au 31-03-2026");
            assertThat(titreDuCsv(service.exportToCsv(null, DEBUT, null, null)))
                .isEqualTo("Liste des retours fournisseur à partir du 05-01-2026");
            assertThat(titreDuCsv(service.exportToCsv(null, null, FIN, null)))
                .isEqualTo("Liste des retours fournisseur jusqu'au 31-03-2026");
            assertThat(titreDuCsv(service.exportToCsv(null, null, null, null))).isEqualTo("Liste des retours fournisseur");
        }

        private void bonsRetournes(RetourBonDTO... bons) {
            when(retourBonService.findAll(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(bons)));
        }

        private RetourBonDTO bon(String reference, RetourBonItemDTO... lignes) {
            RetourBonDTO bon = new RetourBonDTO();
            bon.setId(1);
            bon.setReference(reference);
            bon.setFournisseurLibelle("LABOREX");
            bon.setStatut(RetourStatut.PROCESSING);
            bon.setDateMtv(LocalDateTime.of(2026, 1, 15, 10, 0));
            bon.setRetourBonItems(new ArrayList<>(List.of(lignes)));
            return bon;
        }

        private RetourBonItemDTO ligneRetour(String produit, int quantite, int prixAchat) {
            RetourBonItemDTO ligne = new RetourBonItemDTO();
            ligne.setProduitLibelle(produit);
            ligne.setProduitCip("CIP");
            ligne.setLotNumero("LOT-1");
            ligne.setQtyMvt(quantite);
            ligne.setAcceptedQty(quantite);
            ligne.setPrixAchat(prixAchat);
            ligne.setMotifRetourLibelle("PERIME");
            return ligne;
        }
    }

    // ===== vérification partagée =====

    /**
     * La vérification que l'export du chiffre d'affaires journalier n'aurait pas passée : autant de
     * valeurs que d'en-têtes, et les mêmes en-têtes des deux côtés.
     */
    private static void verifieLaParite(byte[] classeur, byte[] fichierCsv) throws IOException {
        List<String> entetesExcel = entetesDuClasseur(classeur);
        List<String> entetesCsv = entetesDuCsv(fichierCsv);

        assertThat(entetesCsv).isEqualTo(entetesExcel);
        assertThat(premiereDonneeDuCsv(fichierCsv)).hasSameSizeAs(entetesCsv);
        assertThat(premiereDonneeDuClasseur(classeur)).hasSameSizeAs(entetesExcel);
    }

    // ===== lecture des fichiers produits =====

    private static List<String> entetesDuClasseur(byte[] classeur) throws IOException {
        return cellulesDeLaLigne(classeur, 3);
    }

    private static List<String> premiereDonneeDuClasseur(byte[] classeur) throws IOException {
        return cellulesDeLaLigne(classeur, 4);
    }

    private static List<String> cellulesDeLaLigne(byte[] classeur, int ligne) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(classeur))) {
            Sheet feuille = workbook.getSheetAt(0);
            return java.util.stream.StreamSupport.stream(feuille.getRow(ligne).spliterator(), false)
                .map(cellule -> cellule.toString())
                .toList();
        }
    }

    private static List<String> lignesDuCsv(byte[] fichierCsv) {
        // La marque UTF-8 précède le contenu : on la retire avant de lire.
        String contenu = new String(fichierCsv, 3, fichierCsv.length - 3, StandardCharsets.UTF_8);
        return contenu.lines().filter(l -> !l.isBlank()).toList();
    }

    /** Le titre ouvre le fichier, la date le suit, puis viennent les en-têtes. */
    private static String titreDuCsv(byte[] fichierCsv) {
        return lignesDuCsv(fichierCsv).getFirst();
    }

    private static List<String> entetesDuCsv(byte[] fichierCsv) {
        return List.of(lignesDuCsv(fichierCsv).get(2).split(";", -1));
    }

    private static List<String> donneesDuCsv(byte[] fichierCsv) {
        return lignesDuCsv(fichierCsv).subList(3, lignesDuCsv(fichierCsv).size());
    }

    private static List<String> premiereDonneeDuCsv(byte[] fichierCsv) {
        return List.of(lignesDuCsv(fichierCsv).get(3).split(";", -1));
    }
}
