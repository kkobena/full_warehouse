package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.FormatEtiquette;
import com.kobe.warehouse.domain.enumeration.ResolutionEtiquette;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.EtiquetteDTO;
import com.kobe.warehouse.service.etiquette.zpl.impl.ZplEtiquetteServiceImpl;
import com.kobe.warehouse.service.report.Constant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfReader;
import org.thymeleaf.spring6.SpringTemplateEngine;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EtiquetteLotExportReportServiceImpl")
class EtiquetteLotExportReportServiceImplTest {

    private static final String HTML = "<html><head><title>Etiquettes</title></head><body><p>etiquettes</p></body></html>";
    private static final LocalDate PEREMPTION = LocalDate.of(2026, 12, 31);

    /** Le PDF compte en points : 72 par pouce. La seule unite où la page se mesure vraiment. */
    private static final double POINTS_PAR_MM = 72.0 / 25.4;

    /** Les deux supports n'ont ni le même gabarit ni la même géométrie : ils se testent à part. */
    static List<FormatEtiquette> planches() {
        return FormatEtiquette.listerPlanches();
    }

    static List<FormatEtiquette> rouleaux() {
        return FormatEtiquette.listerRouleaux();
    }

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private StorageService storageService;

    @Mock
    private LotRepository lotRepository;

    private EtiquetteLotExportReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EtiquetteLotExportReportServiceImpl(templateEngine, storageService, lotRepository, new ZplEtiquetteServiceImpl());
        Magasin magasin = new Magasin();
        magasin.setId(1);
        magasin.setName("pharmacie du plateau");
        AppUser user = new AppUser();
        user.setMagasin(magasin);
        when(storageService.getUser()).thenReturn(user);
        when(storageService.getConnectedUserMagasin()).thenReturn(magasin);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML);
    }

    private static FournisseurProduit fournisseurProduit(String codeCip) {
        Produit produit = new Produit();
        produit.setId(500);
        produit.setLibelle("DOLIPRANE 1000MG");
        FournisseurProduit fp = new FournisseurProduit();
        fp.setCodeCip(codeCip);
        fp.setPrixUni(1500);
        fp.setProduit(produit);
        produit.setFournisseurProduitPrincipal(fp);
        return fp;
    }

    private static Lot lot(String numLot, LocalDate peremption) {
        Lot lot = new Lot();
        lot.setId(77);
        lot.setNumLot(numLot);
        lot.setExpiryDate(peremption);
        return lot;
    }

    private static OrderLine orderLine(String codeCip, List<Lot> lots) {
        OrderLine line = new OrderLine();
        line.setId(900);
        line.setOrderDate(LocalDate.now());
        line.setFournisseurProduit(fournisseurProduit(codeCip));
        line.setLots(lots);
        return line;
    }

    @SuppressWarnings("unchecked")
    private static List<List<EtiquetteDTO>> paquetsRendus(EtiquetteLotExportReportServiceImpl service) {
        return (List<List<EtiquetteDTO>>) service.getParameters().get(Constant.ITEMS);
    }

    private static List<EtiquetteDTO> etiquettesRenduesDu(EtiquetteLotExportReportServiceImpl service) {
        return paquetsRendus(service).stream().flatMap(List::stream).toList();
    }

    @SuppressWarnings("unchecked")
    private List<EtiquetteDTO> etiquettesRendues() {
        List<List<EtiquetteDTO>> paquets = (List<List<EtiquetteDTO>>) service.getParameters().get(Constant.ITEMS);
        return paquets.stream().flatMap(List::stream).toList();
    }

    @Nested
    @DisplayName("exporterPourBon")
    class ExporterPourBon {

        @Test
        @DisplayName("produit un PDF")
        void produitUnPdf() {
            byte[] pdf = service.exporterPourBon(List.of(orderLine("1234567", List.of(lot("LOT9", PEREMPTION)))), 1);

            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        }

        @Test
        @DisplayName("porte le lot, la peremption et la trame DataMatrix")
        void porteLeLot() {
            service.exporterPourBon(List.of(orderLine("1234567", List.of(lot("LOT9", PEREMPTION)))), 1);

            EtiquetteDTO dto = etiquettesRendues().getFirst();
            assertThat(dto.getCode()).isEqualTo("1234567");
            assertThat(dto.getNumLot()).isEqualTo("LOT9");
            assertThat(dto.getExpiryDate()).isEqualTo("31/12/2026");
            assertThat(dto.getCodeDataMatrix()).isEqualTo("dm:2401234567{GS}10LOT9{GS}17261231");
        }

        @Test
        @DisplayName("une etiquette par lot recu sur la ligne")
        void uneEtiquetteParLot() {
            OrderLine ligne = orderLine("1234567", List.of(lot("LOT1", PEREMPTION), lot("LOT2", PEREMPTION)));

            service.exporterPourBon(List.of(ligne), 1);

            assertThat(etiquettesRendues()).hasSize(2);
            assertThat(etiquettesRendues()).extracting(EtiquetteDTO::getNumLot).containsExactly("LOT1", "LOT2");
        }

        @Test
        @DisplayName("une ligne sans lot ne produit aucune etiquette : c est le role de l etiquette standard")
        void ligneSansLot() {
            service.exporterPourBon(List.of(orderLine("1234567", List.of())), 1);

            assertThat(etiquettesRendues()).isEmpty();
        }

        @Test
        @DisplayName("un lot sans peremption se replie sur le Code 39, sans trame GS1 incomplete")
        void lotSansPeremption() {
            service.exporterPourBon(List.of(orderLine("1234567", List.of(lot("LOT9", null)))), 1);

            EtiquetteDTO dto = etiquettesRendues().getFirst();
            assertThat(dto.getExpiryDate()).isNull();
            assertThat(dto.getCodeDataMatrix()).isEqualTo("dm:2401234567{GS}10LOT9");
        }

        @Test
        @DisplayName("un depart au-dela de la premiere case insere des etiquettes blanches")
        void avecEtiquettesBlanches() {
            service.exporterPourBon(List.of(orderLine("1234567", List.of(lot("LOT9", PEREMPTION)))), 3);

            List<EtiquetteDTO> etiquettes = etiquettesRendues();
            assertThat(etiquettes).hasSize(3);
            assertThat(etiquettes.subList(0, 2)).allSatisfy(e -> assertThat(e.isPrint()).isFalse());
            assertThat(etiquettes.getLast().isPrint()).isTrue();
        }

        @ParameterizedTest(name = "un depart a {0} n insere aucune etiquette blanche")
        @ValueSource(ints = { 0, 1 })
        void sansEtiquetteBlanche(int startAt) {
            service.exporterPourBon(List.of(orderLine("1234567", List.of(lot("LOT9", PEREMPTION)))), startAt);

            assertThat(etiquettesRendues()).hasSize(1);
        }

        @Test
        @DisplayName("decoupe les etiquettes en rangees de cinq")
        void decoupeEnRangeesDeCinq() {
            List<OrderLine> lignes = new java.util.ArrayList<>();
            for (int i = 0; i < 6; i++) {
                lignes.add(orderLine(String.format("%07d", i), List.of(lot("LOT" + i, PEREMPTION))));
            }

            service.exporterPourBon(lignes, 1);

            assertThat(etiquettesRendues()).hasSize(6);
            assertThat(service.getItems()).hasSize(6);
        }
    }

    @Nested
    @DisplayName("exporterPourLot")
    class ExporterPourLot {

        @BeforeEach
        void lotEnBase() {
            Lot lot = lot("LOT9", PEREMPTION);
            lot.setProduit(fournisseurProduit("1234567").getProduit());
            when(lotRepository.findById(77)).thenReturn(Optional.of(lot));
        }

        @Test
        @DisplayName("repete l etiquette autant de fois que demande")
        void repeteLEtiquette() {
            byte[] pdf = service.exporterPourLot(77, 3, 1);

            assertThat(pdf).isNotEmpty();
            assertThat(etiquettesRendues()).hasSize(3);
            assertThat(etiquettesRendues()).allSatisfy(e -> assertThat(e.getNumLot()).isEqualTo("LOT9"));
        }

        @Test
        @DisplayName("mappe le produit, son fournisseur principal et la trame DataMatrix")
        void mappeLeLot() {
            service.exporterPourLot(77, 1, 1);

            EtiquetteDTO dto = etiquettesRendues().getFirst();
            assertThat(dto.getCode()).isEqualTo("1234567");
            assertThat(dto.getLibelle()).isEqualTo("DOLIPRANE 1000MG");
            assertThat(dto.getMagasin()).isEqualTo("PHARMACIE DU PLATEAU");
            assertThat(dto.getPrix()).endsWith("CFA");
            assertThat(dto.getCodeDataMatrix()).isEqualTo("dm:2401234567{GS}10LOT9{GS}17261231");
        }

        @Test
        @DisplayName("un lot introuvable est refuse")
        void lotIntrouvable() {
            when(lotRepository.findById(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.exporterPourLot(404, 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("404");
        }
    }

    @Nested
    @DisplayName("contrat de rapport")
    class ContratDeRapport {

        @Test
        @DisplayName("le nom de fichier genere distingue les etiquettes de lot")
        void nomDeFichier() {
            assertThat(service.getGenerateFileName()).isEqualTo("etiquettes-lot");
        }

        @Test
        @DisplayName("les etiquettes ne sont pas paginees")
        void sansPagination() {
            assertThat(service.getMaxiRowCount()).isZero();
        }

        @Test
        @DisplayName("le rendu passe par le gabarit dedie")
        void gabaritDedie() {
            service.exporterPourBon(List.of(orderLine("1234567", List.of(lot("LOT9", PEREMPTION)))), 1);
            Context context = new Context();

            assertThat(service.getTemplateAsHtml(context)).isEqualTo(HTML);
            assertThat(service.getTemplateAsHtml()).isEqualTo(HTML);
        }
    }

    @Nested
    @DisplayName("genererZpl")
    class GenererZpl {

        @BeforeEach
        void lotEnBase() {
            Lot lot = lot("LOT9", PEREMPTION);
            lot.setProduit(fournisseurProduit("1234567").getProduit());
            when(lotRepository.findById(77)).thenReturn(Optional.of(lot));
        }

        @Test
        @DisplayName("un lot rend un GS1 DataMatrix trace par l imprimante")
        void dataMatrixDuLot() {
            String zpl = service.genererZplPourLot(77, 2, FormatEtiquette.ROULEAU_40X30, ResolutionEtiquette.DPI_203);

            assertThat(zpl).contains("^BXN,");
            // La trame GS1 est la meme que sur planche, ecrite avec les FNC1 du ZPL.
            assertThat(zpl).contains("_12401234567_110LOT9_11726");
            assertThat(zpl).doesNotContain("{GS}");
        }

        @Test
        @DisplayName("un bon rend une etiquette par lot recu")
        void uneEtiquetteParLot() {
            String zpl = service.genererZplPourBon(
                List.of(orderLine("1234567", List.of(lot("LOT1", PEREMPTION), lot("LOT2", PEREMPTION)))),
                FormatEtiquette.ROULEAU_40X30,
                ResolutionEtiquette.DPI_203
            );

            assertThat(zpl).contains("Lot LOT1");
            assertThat(zpl).contains("Lot LOT2");
        }
    }

    @Nested
    @DisplayName("gabarit etiquetes/lot")
    class Gabarit {

        private EtiquetteLotExportReportServiceImpl serviceAvecVraiGabarit;

        @BeforeEach
        void vraiMoteurDeGabarit() {
            ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/");
            resolver.setSuffix(".html");
            resolver.setTemplateMode(TemplateMode.HTML);
            SpringTemplateEngine moteur = new SpringTemplateEngine();
            moteur.setTemplateResolver(resolver);
            serviceAvecVraiGabarit = new EtiquetteLotExportReportServiceImpl(moteur, storageService, lotRepository, new ZplEtiquetteServiceImpl());
        }

        @Test
        @DisplayName("rend l identifiant de la trame DataMatrix, le lot et la peremption")
        void rendLaTrameEtLeLot() {
            serviceAvecVraiGabarit.exporterPourBon(List.of(orderLine("1234567", List.of(lot("LOT9", PEREMPTION)))), 1);

            String html = serviceAvecVraiGabarit.getTemplateAsHtml();

            assertThat(html).contains("dm:2401234567{GS}10LOT9{GS}17261231");
            assertThat(html).contains("Lot LOT9");
            assertThat(html).contains("Exp 31/12/2026");
        }

        @ParameterizedTest(name = "la planche {0} rend sa geometrie et sort un PDF")
        @MethodSource("com.kobe.warehouse.service.stock.impl.EtiquetteLotExportReportServiceImplTest#planches")
        void chaqueFormatRendSaGeometrie(FormatEtiquette format) {
            byte[] pdf = serviceAvecVraiGabarit.exporterPourBon(
                List.of(orderLine("1234567", List.of(lot("LOT9", PEREMPTION)))),
                1,
                format
            );
            String html = serviceAvecVraiGabarit.getTemplateAsHtml();

            assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
            assertThat(html).contains("margin: " + format.getMargeHautMm() + "mm " + format.getMargeCoteMm() + "mm");
            // La case declare sa boite de CONTENU : le retrait de 1 mm s'y ajoute (cf. 3.3 du plan).
            assertThat(html).contains("width: " + format.getLargeurCaseContenuMm() + "mm");
            assertThat(html).contains("height: " + format.getPasVerticalMm() + "mm");
            assertThat(html).contains("width: " + format.getLargeurGrilleMm() + "mm");
        }

        @Test
        @DisplayName("le format par defaut rend la geometrie d avant le catalogue")
        void geometrieHistoriqueParDefaut() {
            serviceAvecVraiGabarit.exporterPourBon(List.of(orderLine("1234567", List.of(lot("LOT9", PEREMPTION)))), 1);

            String html = serviceAvecVraiGabarit.getTemplateAsHtml();

            // Thymeleaf rend un double tel que Java l'écrit : « 5.0mm », CSS valide et même cote.
            // Case de contenu 38 mm + 2 x 1 mm de retrait = pas de 40 mm, le calage historique.
            assertThat(html).contains("margin: 5.0mm 5.0mm");
            assertThat(html).contains("width: 38.0mm");
            assertThat(html).contains("height: 21.2mm");
            assertThat(html).contains("width: 200.0mm");
        }

        @Test
        @DisplayName("le decoupage en rangees suit le nombre de colonnes du format")
        void rangeesSuivantLeFormat() {
            List<OrderLine> lignes = new java.util.ArrayList<>();
            for (int i = 0; i < 6; i++) {
                lignes.add(orderLine(String.format("%07d", i), List.of(lot("LOT" + i, PEREMPTION))));
            }

            serviceAvecVraiGabarit.exporterPourBon(lignes, 1, FormatEtiquette.A4_14_99X38);

            // Deux colonnes : six etiquettes font trois rangees, la ou le format par defaut en fait deux.
            assertThat(paquetsRendus(serviceAvecVraiGabarit)).hasSize(3);
            assertThat(paquetsRendus(serviceAvecVraiGabarit).getFirst()).hasSize(2);
        }

        @ParameterizedTest(name = "le rouleau {0} fixe la page a la taille de l etiquette")
        @MethodSource("com.kobe.warehouse.service.stock.impl.EtiquetteLotExportReportServiceImplTest#rouleaux")
        void chaqueRouleauFixeLaPage(FormatEtiquette format) throws Exception {
            byte[] pdf = serviceAvecVraiGabarit.exporterPourBon(
                List.of(orderLine("1234567", List.of(lot("LOT9", PEREMPTION)))),
                1,
                format
            );
            String html = serviceAvecVraiGabarit.getTemplateAsHtml();

            assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
            // La page EST l'etiquette : une page A4 sortirait une etiquette perdue dans un coin.
            assertThat(html).contains("size: " + format.getLargeurMm() + "mm " + format.getHauteurMm() + "mm");
            assertThat(html).doesNotContain("size: A4");
            // Et le CSS ne prouve rien : c'est la page du PDF produit qui est mesuree.
            PdfReader lecteur = new PdfReader(pdf);
            try {
                Rectangle page = lecteur.getPageSize(1);
                assertThat((double) page.getWidth()).isCloseTo(format.getLargeurMm() * POINTS_PAR_MM, within(1.0));
                assertThat((double) page.getHeight()).isCloseTo(format.getHauteurMm() * POINTS_PAR_MM, within(1.0));
            } finally {
                lecteur.close();
            }
        }

        @Test
        @DisplayName("un rouleau sort une page par etiquette, sans page blanche a la fin")
        void unePageParEtiquetteEnRouleau() throws Exception {
            List<OrderLine> lignes = List.of(
                orderLine("1234567", List.of(lot("LOT1", PEREMPTION), lot("LOT2", PEREMPTION), lot("LOT3", PEREMPTION)))
            );

            byte[] pdf = serviceAvecVraiGabarit.exporterPourBon(lignes, 1, FormatEtiquette.ROULEAU_40X30);

            // Le saut de page est pose AVANT chaque etiquette sauf la premiere : un saut apres
            // la derniere ferait derouler une etiquette blanche a chaque impression.
            PdfReader lecteur = new PdfReader(pdf);
            try {
                assertThat(lecteur.getNumberOfPages()).isEqualTo(3);
            } finally {
                lecteur.close();
            }
        }

        @Test
        @DisplayName("un rouleau ignore la case de depart au lieu de derouler des etiquettes blanches")
        void rouleauIgnoreLaCaseDeDepart() {
            List<OrderLine> lignes = List.of(orderLine("1234567", List.of(lot("LOT9", PEREMPTION))));

            serviceAvecVraiGabarit.exporterPourBon(lignes, 40, FormatEtiquette.ROULEAU_40X30);

            // Sans normalisation, 39 etiquettes blanches auraient defile avant la premiere.
            assertThat(etiquettesRenduesDu(serviceAvecVraiGabarit)).hasSize(1);
            assertThat(etiquettesRenduesDu(serviceAvecVraiGabarit)).allMatch(EtiquetteDTO::isPrint);
        }

        @Test
        @DisplayName("produit un PDF portant le symbole DataMatrix")
        void produitUnPdfAvecLeSymbole() {
            byte[] sansSymbole = serviceAvecVraiGabarit.exporterPourBon(List.of(orderLine("1234567", List.of())), 1);
            byte[] avecSymbole = serviceAvecVraiGabarit.exporterPourBon(
                List.of(orderLine("1234567", List.of(lot("LOT9", PEREMPTION)))),
                1
            );

            assertThat(new String(avecSymbole, 0, 5)).isEqualTo("%PDF-");
            // L'image du symbole est embarquee : le PDF pese plus que la meme planche sans etiquette.
            assertThat(avecSymbole.length).isGreaterThan(sansSymbole.length);
        }
    }
}
